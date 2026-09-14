package com.vincent.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuCreateDTO {
    /** 批量更新时带上，表示更新已有 SKU；为空表示新增 */
    private Long id;
    private Long productId;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
}