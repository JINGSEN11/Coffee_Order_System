package com.vincent.dto;

import lombok.Data;

/**
 * 库存入库入参。
 *
 * 注意字段是 skuId 而不是 productId：库存、售价、预警值都是挂在 sku 上的，
 * 一个商品（尤其规格矩阵商品）下会有几十个 SKU，各自库存独立。
 * 历史实现里这个字段叫 productId，但内部一直按 SKU 主键使用，
 * 调用方很容易顺手传成商品 id 从而「入库到别的 SKU」，所以改回正确的语义。
 */
@Data
public class StockInDTO {
    /** 要入库的 SKU 主键 */
    private Long skuId;
    /** 入库数量（正数） */
    private Integer quantity;
}
