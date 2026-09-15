package com.vincent.interceptor;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.BaseContext;
import com.vincent.common.JwtUtil;
import com.vincent.config.JwtProperties;
import com.vincent.entity.Employee;
import com.vincent.mapper.EmployeeMapper;
import com.vincent.service.PermissionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

/**
 * 管理端鉴权拦截器，绑定 /admin/**。
 *
 * 三道检查，缺一不可：
 * 1. 令牌用**管理端密钥**验签 —— C 端密钥签发的令牌在这里直接签名不通过；
 * 2. 载荷里的 scope 必须是 admin —— 防止将来两把密钥被改成同一个值后身份域失效；
 * 3. 员工当前必须仍存在且状态正常 —— 禁用账号的存量令牌立即失效，不必等它自然过期。
 *
 * 通过后按方法上的 @RequirePerm 校验角色权限。**没有标注注解的接口一律拒绝**（失败关闭），
 * 避免新增接口忘记授权时默认裸奔。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthInterceptor extends AbstractAuthInterceptor {

    private final JwtProperties jwtProperties;
    private final EmployeeMapper employeeMapper;
    private final PermissionService permissionService;

    @Override
    protected String tokenHeaderName() {
        return jwtProperties.getAdminTokenName();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = resolveToken(request);
        if (token == null) {
            log.warn("管理端请求未携带令牌，URI: {}", request.getRequestURI());
            return unauthorized(response, "未登录，请先登录");
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getAdminSecretKey(), token);
        } catch (Exception e) {
            log.warn("管理端令牌无效或已过期，URI: {}", request.getRequestURI());
            return unauthorized(response, "登录已过期，请重新登录");
        }

        if (!BaseContext.SCOPE_ADMIN.equals(claims.get("scope", String.class))) {
            log.warn("非管理端令牌访问管理端接口，URI: {}", request.getRequestURI());
            return unauthorized(response, "登录已过期，请重新登录");
        }

        Long employeeId = toLong(claims.get("id"));
        Employee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null || employee.getStatus() == null || employee.getStatus() != 1) {
            log.warn("员工 {} 不存在或已被禁用，拒绝访问 {}", employeeId, request.getRequestURI());
            return unauthorized(response, "账号已被禁用，请联系管理员");
        }

        BaseContext.set(new BaseContext.Principal(
                employee.getId(), BaseContext.SCOPE_ADMIN, employee.getUsername(), employee.getRoleId()));

        // 非 Controller 方法（静态资源、错误页等）不参与权限判定
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePerm requirePerm = handlerMethod.getMethodAnnotation(RequirePerm.class);
        if (requirePerm == null) {
            requirePerm = handlerMethod.getBeanType().getAnnotation(RequirePerm.class);
        }
        if (requirePerm == null) {
            log.error("管理端接口 {}.{} 未声明 @RequirePerm，已按失败关闭拒绝",
                    handlerMethod.getBeanType().getSimpleName(), handlerMethod.getMethod().getName());
            return forbidden(response, "该接口未配置访问权限");
        }

        String[] required = requirePerm.value();
        if (required.length == 0) {
            return true;
        }
        if (permissionService.hasAny(employee.getRoleId(), required)) {
            return true;
        }

        log.warn("员工 {}（角色 {}）无权访问 {}，需要权限之一：{}",
                employee.getUsername(), employee.getRoleId(),
                request.getRequestURI(), Arrays.toString(required));
        return forbidden(response, "无权限执行该操作，请联系管理员分配权限");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止线程复用导致的身份串号
        BaseContext.remove();
    }

}
