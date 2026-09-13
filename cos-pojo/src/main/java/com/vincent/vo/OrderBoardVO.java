package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderBoardVO {
    private Long pendingCount;
    private Long preparingCount;
    private Long completedCount;
    private Long cancelledCount;
    private Long todayOrderCount;
    private BigDecimal todayRevenue;
}