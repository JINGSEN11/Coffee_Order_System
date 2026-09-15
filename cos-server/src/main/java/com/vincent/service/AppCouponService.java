package com.vincent.service;

import com.vincent.vo.AppCouponPageVO;
import com.vincent.vo.AppUsableCouponListVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * C 端优惠券服务（领券中心 / 我的券 / 领券 / 下单可用券）
 */
public interface AppCouponService {

    /** tab：center（领券中心）| 0（未使用）| 1（已使用）| 2（已过期） */
    AppCouponPageVO list(Long userId, String tab);

    /** 领取优惠券，返回 {claimed, coupon_id} */
    Map<String, Object> claim(Long userId, Long couponId);

    /** 下单可用券（含试算） */
    AppUsableCouponListVO usable(Long userId, BigDecimal amount);

    /** 未使用且未过期的持券数（会员信息 / 首页会员摘要共用） */
    long countUsable(Long userId);
}
