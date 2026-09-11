package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private Long productId;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
}