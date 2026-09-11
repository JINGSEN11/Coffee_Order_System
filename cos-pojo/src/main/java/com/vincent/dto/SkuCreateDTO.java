package com.vincent.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuCreateDTO {
    private Long productId;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
}