package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 规格选项；disabled=true 表示该选项下无有库存的 SKU
 */
@Data
public class AppSpecOptionVO {
    private String name;
    private BigDecimal extra;
    private Boolean disabled;
}
