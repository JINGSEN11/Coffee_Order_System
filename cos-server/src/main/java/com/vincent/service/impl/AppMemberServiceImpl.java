package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.AppMemberUpdateDTO;
import com.vincent.entity.Member;
import com.vincent.entity.Orders;
import com.vincent.entity.PointsRecord;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.PointsRecordMapper;
import com.vincent.service.AppCouponService;
import com.vincent.service.AppMemberService;
import com.vincent.vo.AppMemberVO;
import com.vincent.vo.AppPointsRecordVO;
import com.vincent.vo.AppPointsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppMemberServiceImpl implements AppMemberService {

    private static final int GOLD_THRESHOLD = 1000;
    private static final int SILVER_THRESHOLD = 300;
    private static final int RECORD_LIMIT = 50;

    private final MemberMapper memberMapper;
    private final OrderMapper orderMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final AppCouponService appCouponService;
    private final AppConfigHelper appConfigHelper;

    @Override
    public AppMemberVO memberInfo(Long userId) {
        Member member = userId == null ? null : memberMapper.selectById(userId);
        if (member == null) {
            throw new ServiceException("会员不存在");
        }
        List<Orders> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId)
        );
        // 有效订单数：排除已取消 4、支付失败 5
        long orderCount = orders.stream()
                .filter(o -> o.getStatus() == null || (o.getStatus() != 4 && o.getStatus() != 5))
                .count();
        // 累计实付：状态 1/2/3/6/7
        BigDecimal consumeTotal = orders.stream()
                .filter(o -> o.getStatus() != null && List.of(1, 2, 3, 6, 7).contains(o.getStatus()))
                .map(o -> payAmountOf(o))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int points = member.getPoints() == null ? 0 : member.getPoints();
        AppMemberVO vo = new AppMemberVO();
        vo.setId(member.getId());
        vo.setOpenid(member.getOpenid());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setPhone(member.getPhone());
        vo.setBirthday(member.getBirthday());
        vo.setBirthdayToday(MemberPointsService.isBirthday(member.getBirthday(), LocalDate.now()));
        vo.setBirthdayMultiple(appConfigHelper.intValue("birthday_points_multiple", 2));
        vo.setPoints(points);
        vo.setStatus(member.getStatus());
        vo.setCreatedAt(member.getCreatedAt());
        vo.setOrderCount(orderCount);
        vo.setCouponCount(appCouponService.countUsable(userId));
        vo.setConsumeTotal(AppCalc.money(consumeTotal));
        vo.setLevel(points >= GOLD_THRESHOLD ? "金卡会员" : points >= SILVER_THRESHOLD ? "银卡会员" : "普通会员");
        vo.setNextLevelPoints(points >= GOLD_THRESHOLD ? 0
                : points >= SILVER_THRESHOLD ? GOLD_THRESHOLD - points : SILVER_THRESHOLD - points);
        return vo;
    }

    @Override
    public AppPointsVO points(Long userId) {
        Member member = userId == null ? null : memberMapper.selectById(userId);
        if (member == null) {
            throw new ServiceException("会员不存在");
        }
        int points = member.getPoints() == null ? 0 : member.getPoints();
        int earnRate = appConfigHelper.intValue("points_rate", 100);
        int deductRate = appConfigHelper.intValue("points_deduct_rate", 100);

        List<AppPointsRecordVO> records = pointsRecordMapper.selectList(
                        new LambdaQueryWrapper<PointsRecord>()
                                .eq(PointsRecord::getUserId, userId)
                                .orderByDesc(PointsRecord::getCreatedAt)
                                .last("LIMIT " + RECORD_LIMIT)
                ).stream().map(r -> {
                    AppPointsRecordVO vo = new AppPointsRecordVO();
                    vo.setId(r.getId());
                    vo.setChangeValue(r.getChangeValue());
                    vo.setType(r.getType());
                    vo.setTypeText(AppCalc.pointsTypeText(r.getType()));
                    vo.setOrderId(r.getOrderId());
                    vo.setCreatedAt(r.getCreatedAt());
                    return vo;
                }).collect(Collectors.toList());

        AppPointsVO vo = new AppPointsVO();
        vo.setPoints(points);
        vo.setEarnRate(earnRate);
        vo.setDeductRate(deductRate);
        vo.setMaxDeductAmount(AppCalc.pointsToAmount(points, deductRate));
        vo.setRecords(records.isEmpty() ? new ArrayList<>() : records);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppMemberVO updateProfile(Long userId, AppMemberUpdateDTO dto) {
        Member member = userId == null ? null : memberMapper.selectById(userId);
        if (member == null) {
            throw new ServiceException("会员不存在");
        }
        if (dto == null) {
            return memberInfo(userId);
        }
        if (dto.getBirthday() != null && dto.getBirthday().isAfter(LocalDate.now())) {
            throw new ServiceException("生日不能是未来的日期");
        }

        // 用 UpdateWrapper 而不是 updateById：updateById 会忽略 null 字段，
        // 那样「清除生日」就写不进去（传 null 等于什么都没发生）。
        LambdaUpdateWrapper<Member> update = new LambdaUpdateWrapper<Member>()
                .eq(Member::getId, member.getId())
                .set(Member::getUpdatedAt, LocalDateTime.now());
        if (StringUtils.hasText(dto.getNickname())) {
            update.set(Member::getNickname, dto.getNickname().trim());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            update.set(Member::getAvatar, dto.getAvatar().trim());
        }
        // 生日是全量语义：传了就是设成它，传 null 就是清除
        if (dto.getBirthday() != null) {
            update.set(Member::getBirthday, dto.getBirthday());
        } else {
            update.setSql("birthday = NULL");
        }
        memberMapper.update(null, update);

        log.info("会员 {} 更新资料，生日={}", userId, dto.getBirthday());
        return memberInfo(userId);
    }

    private BigDecimal payAmountOf(Orders order) {        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        return AppCalc.money(amount.subtract(discount).max(BigDecimal.ZERO));
    }
}
