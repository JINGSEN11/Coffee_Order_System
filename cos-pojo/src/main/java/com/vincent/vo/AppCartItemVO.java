package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 购物车行（契约 6.1）
 */
@Data
public class AppCartItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private Long categoryId;
    /** 参与 SKU 价格矩阵的规格键值对（当前为杯型） */
    private Map<String, Object> specs;
    /** 不影响价格的下单选项（温度/糖度），键值对 */
    private Map<String, Object> options;
    /** 规格 + 选项 + 加料的展示文案，如 大杯/冰/半糖 + 燕麦奶 */
    private String specsText;
    /** 加料明细（独立分装商品） */
    private List<AppCartAddonVO> addons;
    /** 不含加料的单价（SKU 价） */
    private BigDecimal basePrice;
    /** 含加料加价的单价（与 price 同值） */
    private BigDecimal unitPrice;
    /** calcAmount 统一读取字段（与 unitPrice 同值） */
    private BigDecimal price;
    private Integer stock;
    private Integer qty;
    private Boolean checked;
    private Long shopId;
}
