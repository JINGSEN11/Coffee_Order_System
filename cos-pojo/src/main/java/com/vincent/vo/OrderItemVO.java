package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long id;
    private Long skuId;
    private String productName;
    private String specs;
    private BigDecimal price;
    private Integer qty;
}