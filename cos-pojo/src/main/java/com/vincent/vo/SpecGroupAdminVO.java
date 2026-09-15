package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 规格组（管理端字典）
 */
@Data
public class SpecGroupAdminVO {
    private Long id;
    /** cup / temp / sugar */
    private String groupKey;
    private String name;
    private Boolean required;
    private Boolean multi;
    /** 是否参与 SKU 价格矩阵：true 会按选项生成 SKU，false 只是下单选项 */
    private Boolean isPriceDim;
    private Integer sort;
    private Integer status;
    private List<SpecOptionAdminVO> options;
}
