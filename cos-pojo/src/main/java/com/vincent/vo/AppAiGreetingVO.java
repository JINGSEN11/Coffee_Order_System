package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 助手「开场白」—— GET /api/app/ai/greeting
 *
 * 页面的空态（还没开始聊）直接渲染这份数据：头像下方的大标题、副标题、
 * 建议问法列表、底部声明。文案与建议问法都放后端，是为了跟着数据变：
 * 比如「现在有哪些券能领」这条只在真有可领券时才下发。
 */
@Data
public class AppAiGreetingVO {

    /** 助手名 */
    private String name;

    /** 空态主标题，如「Hi，我是小卡」 */
    private String title;

    /** 空态副标题，如「你的咖啡点单搭子」 */
    private String subtitle;

    /** 底部声明（AI 生成内容免责） */
    private String disclaimer;

    /** 建议问法 */
    private List<AppAiSuggestionVO> suggestions;
}
