package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品搜索结果（GET /api/app/search）
 */
@Data
public class AppSearchVO {
    private String kw;
    private List<AppProductVO> list;
    private List<String> hotKeywords;
}
