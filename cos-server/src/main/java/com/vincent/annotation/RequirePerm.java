package com.vincent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口的权限声明，取值来自 menu 表 perms 列（如 product:delete、order:refund:audit）。
 *
 * 超级管理员（role_id = 1）持有通配权限 "*:*:*"，恒通过。
 *
 * value 留空表示「只要求是已登录的管理员」—— 用在登录后立刻要调、拿不到角色也谈不上授权的接口上
 * （如 /admin/auth/menus、/admin/upload、数据看板）。
 * 留空是显式声明，不是默认放行：AdminAuthInterceptor 对**没有**加这个注解的管理端接口一律拒绝。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePerm {

    /** 需要的权限码，命中任意一个即放行 */
    String[] value() default {};

}
