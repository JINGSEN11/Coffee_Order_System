package com.vincent.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 助手单轮对话结果 —— POST /api/app/ai/chat
 *
 * 回复文本与卡片分开传：卡片里的商品/券**全部是服务端的真实数据**
 * （商品取 AppProductVO，券取 AppCouponVO），模型只负责「挑哪几个 id」，
 * 不负责编价格、编库存、编有效期 —— 大模型编数字这事没有一次是能容忍的。
 */
@Data
public class AppAiChatVO {

    /** 助手回复文本 */
    private String reply;

    /** 建议追问（点击即发送） */
    private List<AppAiSuggestionVO> suggestions;

    /** 推荐商品卡（真实商品数据，可直接跳详情页下单） */
    private List<AppProductVO> products;

    /** 优惠券卡（真实券数据，卡上带「领取」按钮） */
    private List<AppCouponVO> coupons;

    /**
     * 本轮回复来源：
     * ai    —— 真实模型（DeepSeek）
     * local —— 本地规则兜底（未配 key / 模型调用失败 / 输出不合规）
     * 前端不展示，仅供联调与断言定位「这句话到底谁说的」。
     */
    private String source;
}
