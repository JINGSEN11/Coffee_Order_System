package com.vincent.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductCreateDTO {
    private Long categoryId;
    private String name;
    private String description;
    private String image;
    private String tags;
    private Integer status;
    /** 编辑时前端 inline 传入的 SKU 列表 */
    private List<SkuCreateDTO> skus;
    /**
     * 商品级价格 / 库存，管理端商品编辑页表单在用。
     * 只对「单规格商品」（SKU 数 ≤ 1）生效——多规格商品由 SKU 矩阵定价，
     * 传了也不会改任何 SKU，详见 ProductServiceImpl#createProduct / #updateProduct。
     */
    private java.math.BigDecimal price;
    private Integer stock;
}