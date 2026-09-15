package com.vincent.vo;

import lombok.Data;

/**
 * 首页公告（仅返回契约字段）
 */
@Data
public class AppNoticeVO {
    private Long id;
    private String title;
    private String content;
    private Integer status;
}
