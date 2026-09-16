package com.vincent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明这个管理端接口的操作日志口径（F-A03）。
 *
 * 切面只在「管理端 + 写操作（POST/PUT/DELETE/PATCH）+ 执行成功」时落库，
 * 读接口不会被记录，避免把日志表刷成流水账。
 *
 * 没标注解的控制器也会被记录（模块名从 URI 推断），
 * 标注只是为了把日志写成人看得懂的中文，属于锦上添花而不是前置条件。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 模块名，如「商品」「退款」。留空则从请求 URI 推断 */
    String module() default "";

    /** 动作名，如「审核退款」。留空则用方法名 */
    String action() default "";

    /**
     * 是否把方法入参写进 detail。
     * 涉及密码/密钥的参数无论开关与否都会被剔除（见 OpLogAspect）。
     */
    boolean args() default true;

}
