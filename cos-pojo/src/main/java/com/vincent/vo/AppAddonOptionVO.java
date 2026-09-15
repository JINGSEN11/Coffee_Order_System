package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 加料选项（商品详情 addon_group.options 元素）。
 * 加料是独立的分装商品，因此这里带出它的 SKU、价格与库存，
 * 前端勾选后按 sku_id + qty 提交，价格由服务端复核。
 */
@Data
public class AppAddonOptionVO {
    /** 加料商品对应的 SKU ID（下单/加购时提交这个） */
    private Long skuId;
    /** 加料商品 ID，供前端跳转查看 */
    private Long productId;
    private String name;
    private BigDecimal price;
    /** 加料商品当前库存，0 时 disabled */
    private Integer stock;
    /** 单杯最多加几份 */
    private Integer maxQty;
    private Boolean disabled;
}
