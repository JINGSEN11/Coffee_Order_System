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
        // 1. 尝试从请求头获取管理员 token
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        String secretKey = jwtProperties.getAdminSecretKey();

        // 2. 如果没有管理员 token，则尝试用户端 token
        if (token == null || token.isEmpty()) {
            token = request.getHeader(jwtProperties.getUserTokenName());
            secretKey = jwtProperties.getUserSecretKey();
        }

        // 3. 两个 token 都不存在 → 未登录
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带令牌，URI: {}", request.getRequestURI());
            return unauthorized(response, "未登录，请先登录");
        }

        // 4. 校验 token
        try {
            Claims claims = JwtUtil.parseToken(secretKey, token);
            Long userId = claims.get("id", Long.class);
            log.info("用户 {} 通过令牌验证", userId);
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception e) {
            log.warn("令牌无效或已过期: {}", e.getMessage());
            return unauthorized(response, "登录已过期，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        BaseContext.remove();
    }

    /**
     * 返回 401 未授权响应
     */
    private boolean unauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
        return false;
    }

}