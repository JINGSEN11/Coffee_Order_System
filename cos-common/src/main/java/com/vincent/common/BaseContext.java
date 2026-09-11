package com.vincent.common;

/**
 * 线程级上下文，保存当前登录员工/用户 ID
 */
public class BaseContext {

    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID
     */
    public static void setCurrentId(Long id) {
        THREAD_LOCAL.set(id);
    }

    /**
     * 获取当前线程的用户 ID
     */
    public static Long getCurrentId() {
        return THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的用户 ID（防止内存泄漏）
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }

}