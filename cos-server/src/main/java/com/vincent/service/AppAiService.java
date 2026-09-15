package com.vincent.service;

import com.vincent.dto.AppAiChatDTO;
import com.vincent.vo.AppAiChatVO;
import com.vincent.vo.AppAiGreetingVO;

/**
 * C 端 AI 助手（小卡）：按需求推荐商品 + 引导领券。
 *
 * 设计要点：
 *   · 模型只负责「选 id + 说人话」，商品/券卡片全部由服务端按真实数据装配，
 *     从根上排除「模型编价格、编库存」这类事故；
 *   · 会话不落库，历史由前端每次带上（只取最近 N 条），省掉两张表；
 *   · 未配 api-key 或模型调用失败时降级为本地规则推荐，链路始终可用。
 */
public interface AppAiService {

    /** 开场白：空态文案 + 建议问法（建议问法随「当前有无可领券」动态变化） */
    AppAiGreetingVO greeting(Long userId, Long shopId);

    /** 单轮对话：返回回复文本 + 商品卡 + 券卡 + 建议追问 */
    AppAiChatVO chat(Long userId, AppAiChatDTO dto);
}
