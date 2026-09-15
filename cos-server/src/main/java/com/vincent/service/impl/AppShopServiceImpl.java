package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.entity.Banner;
import com.vincent.entity.Category;
import com.vincent.entity.Member;
import com.vincent.entity.Notice;
import com.vincent.entity.Product;
import com.vincent.entity.Shop;
import com.vincent.mapper.BannerMapper;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.NoticeMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.service.AppCatalogService;
import com.vincent.service.AppCouponService;
import com.vincent.service.AppOrderService;
import com.vincent.service.AppShopService;
import com.vincent.vo.AppBannerVO;
import com.vincent.vo.AppCategoryVO;
import com.vincent.vo.AppHomeVO;
import com.vincent.vo.AppNoticeVO;
import com.vincent.vo.AppProductVO;
import com.vincent.vo.AppShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppShopServiceImpl implements AppShopService {

    private static final int HOME_HOT_LIMIT = 6;
    private static final int HOME_RECOMMEND_LIMIT = 4;
    private static final int HOME_ONGOING_LIMIT = 2;
    private static final String TAG_RECOMMEND = "推荐";

    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final MemberMapper memberMapper;
    private final AppCatalogService appCatalogService;
    private final AppOrderService appOrderService;
    private final AppCouponService appCouponService;
    private final AppShopResolver shopResolver;
    private final AppConfigHelper appConfigHelper;

    @Override
    public AppShopVO shop(Long shopId) {
        Shop shop = shopResolver.resolve(shopId);
        if (shop == null) {
            throw new ServiceException("门店不存在");
        }
        AppShopVO vo = new AppShopVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setAddress(shop.getAddress());
        vo.setPhone(shop.getPhone());
        vo.setBusinessHours(shop.getBusinessHours());
        vo.setAcceptOrder(shop.getAcceptOrder());
        vo.setStatus(shop.getStatus());
        // 接口未提供定位参数，距离/预计时长按契约「无定位权限」返回空串
        vo.setDistanceText("");
        vo.setEtaText("");
        vo.setDeliveryFee(deliveryFee());
        return vo;
    }

    @Override
    public AppHomeVO home(Long shopId, Long userId) {
        Shop shop = shopResolver.resolve(shopId);

        List<AppBannerVO> banners = bannerMapper.selectList(
                        new LambdaQueryWrapper<Banner>()
                                .eq(Banner::getStatus, 1)
                                .orderByAsc(Banner::getSort)
                ).stream().map(b -> {
                    AppBannerVO vo = new AppBannerVO();
                    vo.setId(b.getId());
                    vo.setImageUrl(b.getImageUrl());
                    vo.setLinkUrl(b.getLinkUrl());
                    vo.setSort(b.getSort());
                    vo.setStatus(b.getStatus());
                    return vo;
                }).collect(Collectors.toList());

        List<AppNoticeVO> notices = noticeMapper.selectList(
                        new LambdaQueryWrapper<Notice>()
                                .eq(Notice::getStatus, 1)
                                .orderByDesc(Notice::getCreatedAt)
                ).stream().map(n -> {
                    AppNoticeVO vo = new AppNoticeVO();
                    vo.setId(n.getId());
                    vo.setTitle(n.getTitle());
                    vo.setContent(n.getContent());
                    vo.setStatus(n.getStatus());
                    return vo;
                }).collect(Collectors.toList());

        List<AppCategoryVO> categories = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>()
                                .eq(Category::getStatus, 1)
                                .orderByAsc(Category::getSort)
                ).stream().map(c -> {
                    AppCategoryVO vo = new AppCategoryVO();
                    vo.setId(c.getId());
                    vo.setName(c.getName());
                    vo.setSort(c.getSort());
                    vo.setStatus(c.getStatus());
                    return vo;
                }).collect(Collectors.toList());

        List<Product> hotProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + HOME_HOT_LIMIT)
        );
        List<AppProductVO> hot = appCatalogService.buildProductVOs(hotProducts);

        List<Product> recommendProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .like(Product::getTags, TAG_RECOMMEND)
                        .orderByAsc(Product::getId)
                        .last("LIMIT " + HOME_RECOMMEND_LIMIT)
        );
        List<AppProductVO> recommend = appCatalogService.buildProductVOs(recommendProducts);

        AppHomeVO vo = new AppHomeVO();
        vo.setShop(shopBrief(shop));
        vo.setBanners(banners);
        vo.setNotices(notices);
        vo.setCategories(categories);
        vo.setHot(hot);
        vo.setRecommend(recommend);
        vo.setOngoing(appOrderService.recentOngoing(userId, HOME_ONGOING_LIMIT));
        vo.setMember(memberSummary(userId));
        return vo;
    }

    /* ---------------- 内部方法 ---------------- */

    /** 配送费无对应表列，取 sys_config.delivery_fee，缺省 0.00 */
    private BigDecimal deliveryFee() {
        String raw = appConfigHelper.stringValue("delivery_fee", "0");
        try {
            return AppCalc.money(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            log.warn("sys_config.delivery_fee = {} 无法解析为金额，回退 0.00", raw);
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    private Map<String, Object> shopBrief(Shop shop) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", shop == null ? null : shop.getId());
        map.put("name", shop == null ? "" : shop.getName());
        map.put("accept_order", shop == null ? null : shop.getAcceptOrder());
        return map;
    }

    /** 会员摘要：{id, nickname, points, coupon_count}，券数按「未使用且未过期」实时判定 */
    private Map<String, Object> memberSummary(Long userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        Member member = userId == null ? null : memberMapper.selectById(userId);
        map.put("id", member == null ? null : member.getId());
        map.put("nickname", member == null ? null : member.getNickname());
        map.put("points", member == null ? 0 : member.getPoints());
        map.put("coupon_count", appCouponService.countUsable(userId));
        return map;
    }
}
