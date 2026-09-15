package com.vincent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单明细（契约 13.3 items 元素）
 * 加料在库里是独立明细行（item_type=2 + parent_item_id 指回主商品行），
 * 展示时归并到主商品行的 addons 上，避免出现「燕麦奶 ¥3」这种孤立行。
 */
@Data
public class AppOrderItemVO {
    private Long id;
    private Long orderId;
    private Long skuId;
    /** 由 order_item.sku_id 反查 SKU 补全，供跳转商品详情用 */
    private Long productId;
    private Long categoryId;
    private String productName;
    private String specs;
    private BigDecimal price;
    private Integer qty;
    /** 该行挂的加料（来自 item_type=2 的子行） */
    private List<AppCartAddonVO> addons;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
