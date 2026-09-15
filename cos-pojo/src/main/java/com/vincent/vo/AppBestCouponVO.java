package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单可用券的「首张可用券」摘要
 */
@Data
public class AppBestCouponVO {
    private Long userCouponId;
    private BigDecimal discountPreview;
    private Boolean usable;
}
