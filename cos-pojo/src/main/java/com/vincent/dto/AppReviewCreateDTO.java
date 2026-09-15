package com.vincent.dto;

import lombok.Data;

import java.util.List;

/**
 * 提交评价入参（POST /api/app/order/{id}/review）
 */
@Data
public class AppReviewCreateDTO {
    private Integer score;
    private String content;
    /** 快捷标签，落库时以「、」拼接到 content 前缀（review 表无 tags 列） */
    private List<String> tags;
    /** 匿名评价标记，review 表无对应列，仅记录日志 */
    private Boolean anonymous;
}
