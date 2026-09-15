package com.vincent.vo;

import lombok.Data;

/**
 * 商品分类（仅返回契约字段）
 */
@Data
public class AppCategoryVO {
    private Long id;
    private String name;
    private Integer sort;
    private Integer status;
}
