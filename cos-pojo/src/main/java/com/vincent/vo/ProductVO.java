package com.vincent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String image;
    private String tags;
    private Integer sales;
    private Integer status;
    private LocalDateTime createdAt;
    /** 前端商品列表（List.vue）需要的聚合字段 */
    private List<SkuVO> skus;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private Integer skuCount;
    private Integer stock;
    private Boolean lowStock;
}