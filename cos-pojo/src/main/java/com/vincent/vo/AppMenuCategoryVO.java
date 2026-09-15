package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 菜单分类（含该分类下的商品）
 */
@Data
public class AppMenuCategoryVO {
    private Long id;
    private String name;
    private Integer sort;
    private Integer status;
    private Integer count;
    private List<AppProductVO> products;
}
