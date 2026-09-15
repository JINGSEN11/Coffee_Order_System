package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 规格组（杯型 / 温度 / 糖度）
 * priceDim = true 的组参与 SKU 价格矩阵（选项的 extra 有意义、disabled 取决于 SKU 库存）；
 * priceDim = false 的组只是下单选项，不影响价格与库存。
 */
@Data
public class AppSpecGroupVO {
    /** 组标识 cup/temp/sugar */
    private String key;
    private String label;
    private Boolean required;
    /** 是否多选 */
    private Boolean multi;
    /** 是否影响价格（false 表示该组选项只是制作口径，价格与库存都不变） */
    private Boolean priceDim;
    private List<AppSpecOptionVO> options;
}
