package com.vincent.vo;

import lombok.Data;

/**
 * 首页轮播图（仅返回契约字段，不含 created_at 等管理端字段）
 */
@Data
public class AppBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private Integer status;
}
