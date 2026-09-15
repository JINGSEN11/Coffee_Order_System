package com.vincent.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 菜单聚合（GET /api/app/menu）
 */
@Data
public class AppMenuVO {
    /** 门店简要信息 {id, name, accept_order} */
    private Map<String, Object> shop;
    private List<AppMenuCategoryVO> categories;
    private AppCartVO cart;
}
