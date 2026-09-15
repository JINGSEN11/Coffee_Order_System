package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车行里的加料项（契约 6.1 addons 元素）。
 * 名称与价格由服务端按加料 SKU 实时查表装配，不落快照。
 */
@Data
public class AppCartAddonVO {
    private Long skuId;
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer qty;
}
