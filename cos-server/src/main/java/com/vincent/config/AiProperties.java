package com.vincent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 助手（小卡）的**业务**配置（cos.ai.*）。
 *
 * 这里只放「跟模型无关、只跟我们的业务口径有关」的旋钮。
 * 连接与模型参数（api-key / base-url / model / temperature / max-tokens / 重试）
 * 一律走 Spring AI 官方的 {@code spring.ai.deepseek.*}，
 * 由 spring-ai-starter-model-deepseek 的自动装配接管，不在这里另起一套，免得两边漂移。
 */
@Component
@ConfigurationProperties(prefix = "cos.ai")
@Data
public class AiProperties {

    /** 总开关：false 时即使模型可用也强制走本地兜底（用于对比效果或演练降级） */
    private boolean enabled = true;

    /** 带上文的最大轮数（不含本轮） */
    private int maxHistory = 8;

    /** 塞进提示词的商品数量上限（按销量倒序截断，控制 token 与响应延迟） */
    private int catalogSize = 60;

    /** 单轮最多返回的商品卡数 */
    private int maxProducts = 3;

    /** 单轮最多返回的券卡数 */
    private int maxCoupons = 2;
}
