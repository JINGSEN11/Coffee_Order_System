package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.entity.Coupon;
import com.vincent.entity.UserCoupon;
import com.vincent.mapper.CouponMapper;
import com.vincent.mapper.UserCouponMapper;
import com.vincent.service.AppCouponService;
import com.vincent.vo.AppBestCouponVO;
import com.vincent.vo.AppCouponPageVO;
import com.vincent.vo.AppCouponVO;
import com.vincent.vo.AppUsableCouponListVO;
import com.vincent.vo.AppUsableCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppCouponServiceImpl implements AppCouponService {

    private static final String TAB_CENTER = "center";
    private static final String REMAIN_EXPIRED = "已过期";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public AppCouponPageVO list(Long userId, String tab) {
        String currentTab = (tab == null || tab.isBlank()) ? TAB_CENTER : tab.trim();
        List<UserCoupon> mine = userId == null ? new ArrayList<>() : userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByAsc(UserCoupon::getId)
        );

        AppCouponPageVO page = new AppCouponPageVO();
        page.setTab(currentTab);

        if (TAB_CENTER.equals(currentTab)) {
            List<Coupon> coupons = couponMapper.selectList(
                    new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus, 1).orderByAsc(Coupon::getId)
            );
            List<AppCouponVO> list = new ArrayList<>();
            for (Coupon coupon : coupons) {
                AppCouponVO vo = base(coupon);
                int claimed = (int) mine.stream()
                        .filter(uc -> Objects.equals(uc.getCouponId(), coupon.getId()))
                        .count();
                int remaining = coupon.getTotalCount() == null || coupon.getTotalCount() == 0
                        ? -1
                        : Math.max(0, coupon.getTotalCount()
                        - (coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount()));
                int limit = coupon.getPerUserLimit() == null ? 1 : coupon.getPerUserLimit();
                vo.setRemaining(remaining);
                vo.setClaimedCount(claimed);
                vo.setCanClaim(claimed < limit && (remaining != 0));
                list.add(vo);
            }
            page.setList(list);
            page.setClaimedIds(mine.stream().map(UserCoupon::getCouponId).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList()));
            return page;
        }

        int wanted;
        try {
            wanted = Integer.parseInt(currentTab);
        } catch (NumberFormatException e) {
            throw new ServiceException("券类型参数不合法");
        }

        Map<Long, Coupon> coupons = couponsById(mine.stream().map(UserCoupon::getCouponId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        List<AppCouponVO> list = new ArrayList<>();
        for (UserCoupon userCoupon : mine) {
            Coupon coupon = coupons.get(userCoupon.getCouponId());
            if (coupon == null) {
                continue;
            }
            int ucStatus = ucStatusOf(userCoupon, coupon);
            if (ucStatus != wanted) {
                continue;
            }
            AppCouponVO vo = base(coupon);
            vo.setUserCouponId(userCoupon.getId());
            vo.setUcStatus(ucStatus);
            vo.setRemainText(remainText(coupon, ucStatus));
            vo.setClaimedAt(userCoupon.getClaimedAt());
            vo.setUsedAt(userCoupon.getUsedAt());
            list.add(vo);
        }
        page.setList(list);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> claim(Long userId, Long couponId) {
        Coupon coupon = couponId == null ? null : couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new ServiceException("券不存在");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new ServiceException("该券已下架");
        }
        if (coupon.getTotalCount() != null && coupon.getTotalCount() > 0
                && (coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount()) >= coupon.getTotalCount()) {
            throw new ServiceException("该券已被领完");
        }
        int limit = coupon.getPerUserLimit() == null ? 1 : coupon.getPerUserLimit();
        long claimed = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, coupon.getId())
        );
        if (claimed >= limit) {
            throw new ServiceException("每人限领 " + limit + " 张，你已领过啦");
        }
        // 并发兜底：原子占用发放名额
        if (couponMapper.incrementReceivedIfAvailable(coupon.getId()) == 0) {
            throw new ServiceException("该券已被领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setStatus(0);
        userCoupon.setClaimedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);

        Map<String, Object> result = new HashMap<>();
        result.put("claimed", true);
        result.put("coupon_id", coupon.getId());
        log.info("会员 {} 领取优惠券 {}", userId, coupon.getId());
        return result;
    }

    @Override
    public AppUsableCouponListVO usable(Long userId, BigDecimal amount) {
        BigDecimal goodsAmount = AppCalc.money(amount);
        List<UserCoupon> mine = userId == null ? new ArrayList<>() : userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
        );
        Map<Long, Coupon> coupons = couponsById(mine.stream().map(UserCoupon::getCouponId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        LocalDateTime now = LocalDateTime.now();
        List<AppUsableCouponVO> list = new ArrayList<>();
        for (UserCoupon userCoupon : mine) {
            Coupon coupon = coupons.get(userCoupon.getCouponId());
            if (coupon == null) {
                continue;
            }
            // 过期由 valid_end_time 实时判定，不依赖落库状态
            if (coupon.getValidEndTime() != null && coupon.getValidEndTime().isBefore(now)) {
                continue;
            }
            BigDecimal preview = AppCalc.couponAmountOf(coupon, goodsAmount);
            AppUsableCouponVO vo = new AppUsableCouponVO();
            vo.setId(coupon.getId());
            vo.setName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setThresholdAmount(coupon.getThresholdAmount());
            vo.setDiscountAmount(coupon.getDiscountAmount());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setUserCouponId(userCoupon.getId());
            vo.setDiscountPreview(preview);
            vo.setUsable(preview.compareTo(BigDecimal.ZERO) > 0);
            vo.setRemainText(remainText(coupon, 0));
            list.add(vo);
        }
        list.sort(Comparator
                .comparing((AppUsableCouponVO v) -> Boolean.TRUE.equals(v.getUsable()) ? 0 : 1)
                .thenComparing(v -> v.getDiscountPreview().negate()));

        AppUsableCouponVO firstUsable = list.stream()
                .filter(v -> Boolean.TRUE.equals(v.getUsable()))
                .findFirst().orElse(null);

        AppUsableCouponListVO vo = new AppUsableCouponListVO();
        vo.setList(list);
        if (firstUsable != null) {
            AppBestCouponVO best = new AppBestCouponVO();
            best.setUserCouponId(firstUsable.getUserCouponId());
            best.setDiscountPreview(firstUsable.getDiscountPreview());
            best.setUsable(firstUsable.getUsable());
            vo.setBest(best);
        }
        return vo;
    }

    /* ---------------- 内部方法 ---------------- */

    @Override
    public long countUsable(Long userId) {
        if (userId == null) {
            return 0L;
        }
        List<UserCoupon> mine = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
        );
        if (mine.isEmpty()) {
            return 0L;
        }
        Map<Long, Coupon> coupons = couponsById(mine.stream().map(UserCoupon::getCouponId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        LocalDateTime now = LocalDateTime.now();
        return mine.stream().filter(uc -> {
            Coupon coupon = coupons.get(uc.getCouponId());
            return coupon != null && (coupon.getValidEndTime() == null || coupon.getValidEndTime().isAfter(now));
        }).count();
    }

    private AppCouponVO base(Coupon coupon) {
        AppCouponVO vo = new AppCouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setThresholdAmount(AppCalc.money(coupon.getThresholdAmount()));
        vo.setDiscountAmount(coupon.getDiscountAmount());
        vo.setDiscountRate(coupon.getDiscountRate());
        vo.setTotalCount(coupon.getTotalCount());
        vo.setReceivedCount(coupon.getReceivedCount());
        vo.setPerUserLimit(coupon.getPerUserLimit());
        vo.setValidStartTime(coupon.getValidStartTime());
        vo.setValidEndTime(coupon.getValidEndTime());
        vo.setStatus(coupon.getStatus());
        return vo;
    }

    /** 持券状态：已使用的仍为已使用；未使用但已过 valid_end_time 的实时判为已过期 */
    private int ucStatusOf(UserCoupon userCoupon, Coupon coupon) {
        int stored = userCoupon.getStatus() == null ? 0 : userCoupon.getStatus();
        if (stored == 0 && coupon.getValidEndTime() != null
                && coupon.getValidEndTime().isBefore(LocalDateTime.now())) {
            return 2;
        }
        return stored;
    }

    private String remainText(Coupon coupon, int ucStatus) {
        if (ucStatus == 2) {
            return REMAIN_EXPIRED;
        }
        if (coupon.getValidEndTime() == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), coupon.getValidEndTime());
        if (days < 0) {
            return REMAIN_EXPIRED;
        }
        if (days == 0) {
            return "今天到期";
        }
        return days + " 天后到期";
    }

    private Map<Long, Coupon> couponsById(List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return new HashMap<>();
        }
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds))
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c, (a, b) -> a));
    }
}
