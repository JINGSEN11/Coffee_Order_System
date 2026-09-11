package com.vincent.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT Token 生成与校验工具类
 */
public class JwtUtil {

    /**
     * 生成 JWT Token
     *
     * @param secretKey  密钥
     * @param ttlMillis  过期时间（毫秒）
     * @param claims     自定义载荷
     * @return JWT Token 字符串
     */
    public static String createToken(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + ttlMillis);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param secretKey 密钥
     * @param token     JWT Token 字符串
     * @return 载荷 Claims
     */
    public static Claims parseToken(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效
     *
     * @param secretKey 密钥
     * @param token     JWT Token 字符串
     * @return true=有效, false=无效
     */
    public static boolean validateToken(String secretKey, String token) {
        try {
            parseToken(secretKey, token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}