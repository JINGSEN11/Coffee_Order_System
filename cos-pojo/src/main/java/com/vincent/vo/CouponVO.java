package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer perUserLimit;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer status;
    private LocalDateTime createdAt;
}