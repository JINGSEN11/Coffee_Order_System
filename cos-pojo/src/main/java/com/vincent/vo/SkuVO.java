package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private Long productId;
    /**
     * 所属商品名称。sku 表本身没有名称列，库存/预警列表如果只回 productId，
     * 前端只能显示成 "SKU-3"，既看不出是哪个商品也没法核对规格，所以在这里补上。
     */
    private String productName;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private Integer warnStock;
}