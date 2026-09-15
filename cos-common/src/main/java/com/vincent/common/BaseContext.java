package com.vincent.common;

/**
 * 线程级上下文，保存当前请求的登录身份。
 *
 * 管理端与 C 端共用同一个 ThreadLocal，id 的语义由 scope 决定：
 * scope=admin 时是 employee.id，scope=user 时是 member.id。
 * 两个 id 分属不同的主键空间，必须靠 scope 区分，不能只看 id 数字。
 *
 * 写入方只有两个拦截器（AdminAuthInterceptor / AppAuthInterceptor），
 * 业务代码一律只读。
 */
public class BaseContext {

    /** 管理端身份域 */
    public static final String SCOPE_ADMIN = "admin";
    /** C 端（小程序会员）身份域 */
    public static final String SCOPE_USER = "user";

    /** 当前请求的身份；未登录时为 null */
    public record Principal(Long id, String scope, String username, Long roleId) {

        public boolean isAdmin() {
            return SCOPE_ADMIN.equals(scope);
        }

        public boolean isSuperAdmin() {
            // 与 AuthServiceImpl 的约定保持一致：role_id = 1 为内置超级管理员
            return isAdmin() && roleId != null && roleId == 1L;
        }
    }

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    /** 由拦截器在鉴权通过后写入 */
    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    /** 当前身份，未登录返回 null */
    public static Principal get() {
        return HOLDER.get();
    }

    /**
     * 当前登录主体 ID。
     * 管理端接口拿到的是 employee.id，C 端接口拿到的是 member.id ——
     * 取值前若不确认身份域，跨端调用会拿到同号的另一个主体。
     */
    public static Long getCurrentId() {
        Principal principal = HOLDER.get();
        return principal == null ? null : principal.id();
    }

    /** 当前身份域，未登录返回 null */
    public static String getScope() {
        Principal principal = HOLDER.get();
        return principal == null ? null : principal.scope();
    }

    /** 当前登录员工的角色 ID；C 端返回 null */
    public static Long getRoleId() {
        Principal principal = HOLDER.get();
        return principal == null ? null : principal.roleId();
    }

    /** 请求结束后清理，防止线程复用导致的身份串号与内存泄漏 */
    public static void remove() {
        HOLDER.remove();
    }

}
