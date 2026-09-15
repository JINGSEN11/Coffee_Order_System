package com.vincent.dto;

import lombok.Data;

/**
 * 加料项（加入购物车请求体中的 addons 元素）。
 * 加料现在是**独立的分装商品**（有自己的 SKU / 价格 / 库存），
 * 所以请求体只需要引用它的 SKU 与份数，名称与价格一律由服务端实时查表得出，
 * 避免前端传价带来的价格欺骗与快照过期。
 */
@Data
public class AppAddonDTO {
    /** 加料商品对应的 SKU ID */
    private Long skuId;
    /** 份数，默认 1 */
    private Integer qty;
}
