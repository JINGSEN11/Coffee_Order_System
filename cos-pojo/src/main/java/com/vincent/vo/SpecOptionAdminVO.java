package com.vincent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 规格选项（管理端字典）
 */
@Data
public class SpecOptionAdminVO {
    private Long id;
    private String name;
    /** 加价，仅 is_price_dim 的组有意义 */
    private BigDecimal extra;
    private Integer sort;
    private Integer status;
}
