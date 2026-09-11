package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardVO {
    private Long todayOrderCount;
    private BigDecimal todayRevenue;
    private Long totalMemberCount;
    private Long totalProductCount;
    private Long pendingOrderCount;
    private Long pendingReviewCount;
}