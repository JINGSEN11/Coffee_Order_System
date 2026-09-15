package com.vincent.interceptor;

import com.vincent.common.BaseContext;
import com.vincent.common.JwtUtil;
import com.vincent.config.JwtProperties;
import com.vincent.entity.Member;
import com.vincent.mapper.MemberMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * C 端（小程序会员）鉴权拦截器，绑定 /api/app/**。
 *
 * 与管理端同理：只认**用户端密钥**签发且 scope=user 的令牌。
 * 会员被禁用后，其存量令牌立即失效 —— 与 AppAuthServiceImpl 登录时的状态判定口径一致
 * （能登录的就能继续用，登录时被拒的下次请求就会被拒）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppAuthInterceptor extends AbstractAuthInterceptor {

    private final JwtProperties jwtProperties;
    private final MemberMapper memberMapper;

    @Override
    protected String tokenHeaderName() {
        return jwtProperties.getUserTokenName();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = resolveToken(request);
        if (token == null) {
            log.warn("C 端请求未携带令牌，URI: {}", request.getRequestURI());
            return unauthorized(response, "未登录，请先登录");
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getUserSecretKey(), token);
        } catch (Exception e) {
            log.warn("C 端令牌无效或已过期，URI: {}", request.getRequestURI());
            return unauthorized(response, "登录已过期，请重新登录");
        }

        if (!BaseContext.SCOPE_USER.equals(claims.get("scope", String.class))) {
            log.warn("非 C 端令牌访问小程序接口，URI: {}", request.getRequestURI());
            return unauthorized(response, "登录已过期，请重新登录");
        }

        Long memberId = toLong(claims.get("id"));
        Member member = memberId == null ? null : memberMapper.selectById(memberId);
        if (member == null || (member.getStatus() != null && member.getStatus() != 1)) {
            log.warn("会员 {} 不存在或已被禁用，拒绝访问 {}", memberId, request.getRequestURI());
            return unauthorized(response, "账号状态异常，请重新登录");
        }

        BaseContext.set(new BaseContext.Principal(
                member.getId(), BaseContext.SCOPE_USER, member.getNickname(), null));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.remove();
    }

}
