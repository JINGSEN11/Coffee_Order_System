package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * C 端商品列表项（契约 13.1 ProductVO）
 */
@Data
public class AppProductVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String image;
    /** 逗号分隔原文，如 新品,推荐 */
    private String tags;
    private List<String> tagList;
    private Integer sales;
    private Integer status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer skuCount;
    /** 全部 SKU 库存为 0 */
    private Boolean soldOut;
    /** 有库存但全部低于预警值 */
    private Boolean lowStock;
    /** 评价均分，无评价时为 4.9 */
    private BigDecimal reviewScore;
}
