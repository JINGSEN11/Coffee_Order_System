package com.vincent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cos.jwt")
@Data
public class JwtProperties {

    /**
     * 管理员 JWT 密钥
     */
    private String adminSecretKey;

    /**
     * 管理员 JWT 过期时间（毫秒）
     */
    private long adminTtl;

    /**
     * 管理员 token 请求头名称
     */
    private String adminTokenName;

    /**
     * 用户端 JWT 密钥
     */
    private String userSecretKey;

    /**
     * 用户端 JWT 过期时间（毫秒）
     */
    private long userTtl;

    /**
     * 用户端 token 请求头名称
     */
    private String userTokenName;

}