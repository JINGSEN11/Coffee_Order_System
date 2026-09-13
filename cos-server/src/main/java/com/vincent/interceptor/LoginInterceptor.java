package com.vincent.interceptor;

import com.vincent.common.BaseContext;
import com.vincent.common.JwtUtil;
import com.vincent.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);

        if (token == null || token.isEmpty()) {
            log.warn("请求未携带令牌，URI: {}", request.getRequestURI());
            return unauthorized(response, "未登录，请先登录");
        }

        // 管理员密钥 / 用户端密钥依次尝试，任一校验通过即放行
        String[] secrets = {jwtProperties.getAdminSecretKey(), jwtProperties.getUserSecretKey()};
        for (String secret : secrets) {
            try {
                Claims claims = JwtUtil.parseToken(secret, token);
                Object idObj = claims.get("id");
                Long userId = idObj == null ? null : Long.valueOf(String.valueOf(idObj));
                log.info("用户 {} 通过令牌验证", userId);
                BaseContext.setCurrentId(userId);
                return true;
            } catch (Exception ignored) {
                // 换下一个密钥重试
            }
        }

        log.warn("令牌无效或已过期，URI: {}", request.getRequestURI());
        return unauthorized(response, "登录已过期，请重新登录");
    }

    /**
     * 解析请求中的令牌。优先取标准 Authorization: Bearer 头（前端 axios 默认携带），
     * 其次兼容 admin-token-name / user-token-name 指定的自定义头。
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String t = authHeader.substring(7).trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        if (token == null || token.isEmpty()) {
            token = request.getHeader(jwtProperties.getUserTokenName());
        }
        return token;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        BaseContext.remove();
    }

    /**
     * 返回 401 未授权响应（字段名与前端 request.js 约定一致：code / message）
     */
    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }

}
