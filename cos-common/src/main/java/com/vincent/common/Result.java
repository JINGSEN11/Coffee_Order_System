package com.vincent.common;

import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    /** 与前端管理端（src/api/request.js）约定：code=0 为成功，非 0 一律按失败处理 */
    private static final Integer SUCCESS_CODE = 0;
    private static final Integer ERROR_CODE = 500;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(ERROR_CODE, "操作失败", null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ERROR_CODE, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

}