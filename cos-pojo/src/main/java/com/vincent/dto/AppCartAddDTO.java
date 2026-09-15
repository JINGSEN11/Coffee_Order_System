package com.vincent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 加入购物车入参（POST /api/app/cart）
 */
@Data
public class AppCartAddDTO {
    private Long skuId;
    private Integer qty;
    /**
     * 不影响价格的下单选项（温度/糖度），如 {"温度":"冰","糖度":"半糖"}。
     * 它们不参与 SKU 价格矩阵，但参与购物车行的区分与唯一键。
     */
    private Map<String, Object> options;
    /** 加料明细（独立分装商品的 SKU + 份数） */
    private List<AppAddonDTO> addons;
    /**
     * 门店 ID。契约请求体未定义该字段，缺省时取默认门店（库中第一条门店）；
     * 保留为可选字段是因为购物车行需要 shop_id 参与合并判定
     * （user_id + sku_id + shop_id + options_hash + addons_hash）。
     */
    private Long shopId;
}
