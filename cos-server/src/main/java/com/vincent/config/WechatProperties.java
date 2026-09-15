package com.vincent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置（cos.wechat.*）。
 * appid / secret 为空时，C 端登录走「code 哈希兜底」便于开发环境独立联调。
 */
@Component
@ConfigurationProperties(prefix = "cos.wechat")
@Data
public class WechatProperties {

    /** 小程序 AppID */
    private String appid;

    /** 小程序 AppSecret */
    private String secret;
}
