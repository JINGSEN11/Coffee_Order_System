package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单可用券条目（GET /api/app/coupons/usable）
 */
@Data
public class AppUsableCouponVO {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Long userCouponId;
    private BigDecimal discountPreview;
    private Boolean usable;
    /** 到期提示文案，如「12 天后到期」「今天到期」（与「我的券」保持同源） */
    private String remainText;
}
