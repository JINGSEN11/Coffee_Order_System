package com.vincent.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 鉴权拦截器的公共部分：取令牌、写错误响应。
 *
 * 两个子类各自绑定一条路径前缀和一把密钥，**互不通融** ——
 * 这是修复「一个 token 打通两端」的关键：管理端只认管理端密钥签发的令牌，
 * C 端只认 C 端密钥签发的令牌，密钥不同即签名校验不通过。
 */
public abstract class AbstractAuthInterceptor implements HandlerInterceptor {

    /** 令牌请求头名称（管理端 token / 用户端 authentication），与前端约定一致 */
    protected abstract String tokenHeaderName();

    /**
     * 解析请求中的令牌。优先取标准 Authorization: Bearer 头（前端 axios 默认携带），
     * 其次兼容自定义头。
     */
    protected String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        String custom = request.getHeader(tokenHeaderName());
        return (custom == null || custom.isEmpty()) ? null : custom;
    }

    /**
     * 写出鉴权失败响应并中断请求。
     * 字段名与前端 request.js 约定一致：code / message。
     */
    protected boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }

    /** 401：没登录 / 登录过期 / 身份域不对 */
    protected boolean unauthorized(HttpServletResponse response, String message) throws IOException {
        return reject(response, 401, message);
    }

    /** 403：登录了但没这个权限 */
    protected boolean forbidden(HttpServletResponse response, String message) throws IOException {
        return reject(response, 403, message);
    }

    /** 统一的 Long 取值：JWT 里的数字经 JSON 反序列化可能是 Integer 或 Long */
    protected Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

}
