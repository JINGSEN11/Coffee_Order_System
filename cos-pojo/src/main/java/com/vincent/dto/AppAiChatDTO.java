package com.vincent.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 助手对话入参 —— POST /api/app/ai/chat
 */
@Data
public class AppAiChatDTO {

    /** 本轮用户输入 */
    private String message;

    /** 历史消息（不含本轮），按时间正序；服务端只取最近 N 条 */
    private List<AppAiMessageDTO> history;

    /** 门店 ID，用于取门店名/营业时间等上下文 */
    private Long shopId;
}
