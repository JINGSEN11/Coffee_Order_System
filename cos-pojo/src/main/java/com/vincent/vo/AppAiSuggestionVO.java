package com.vincent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 助手的「建议问法」条目
 *
 * icon 由后端下发（emoji 字符），前端不做「第几条配什么图标」的映射 ——
 * 建议问法是后端随数据动态生成的，前端按下标猜图标迟早会错位。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppAiSuggestionVO {

    /** 图标（emoji 字符，如 ☕ ），前端直接当文字渲染 */
    private String icon;

    /** 展示文案，同时就是用户点击后发出的问题 */
    private String text;
}
