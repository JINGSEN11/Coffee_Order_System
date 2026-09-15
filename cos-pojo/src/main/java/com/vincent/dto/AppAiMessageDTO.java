package com.vincent.dto;

import lombok.Data;

/**
 * 单条历史消息（AI 助手的上下文）
 *
 * 会话不落库：历史由前端在每次请求时一起带上，服务端只取最近 N 条。
 * 这样省掉两张表（session / message）与一套清理逻辑，
 * 代价是换设备不同步 —— 聊天记录本来也不是必须持久化的业务数据。
 */
@Data
public class AppAiMessageDTO {

    /** user | assistant */
    private String role;

    /** 消息内容 */
    private String content;
}
