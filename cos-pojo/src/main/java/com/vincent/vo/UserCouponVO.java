package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {
    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer status;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
}