package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 加料组（addon_group）：由 product_addon 关联的加料商品装配而来，
 * 不参与 SKU 价格矩阵，下单时展开为独立的订单明细行。
 */
@Data
public class AppAddonGroupVO {
    private String key;
    private String label;
    private Boolean required;
    /** 是否可多选 */
    private Boolean multi;
    private List<AppAddonOptionVO> options;
}
