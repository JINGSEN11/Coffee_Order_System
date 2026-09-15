package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 可用券响应（GET /api/app/coupons/usable）
 */
@Data
public class AppUsableCouponListVO {
    private List<AppUsableCouponVO> list;
    /** 首张可用券，未命中时返回 null */
    private AppBestCouponVO best;
}
