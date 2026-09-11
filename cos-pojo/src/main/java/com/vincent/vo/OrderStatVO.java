package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatVO {
    private String date;
    private Long orderCount;
    private BigDecimal revenue;
    private BigDecimal avgOrderAmount;
}