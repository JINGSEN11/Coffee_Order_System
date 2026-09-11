package com.vincent.common.exception;

import com.vincent.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * 全局异常捕获，返回统一格式错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 自定义业务异常 */
    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleServiceException(ServiceException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** SQL 异常 */
    @ExceptionHandler(SQLException.class)
    public Result<Void> handleSQLException(SQLException e) {
        log.error("数据库异常：{}", e.getMessage());
        return Result.error("数据库操作异常，请稍后重试");
    }

    /** 参数校验异常 */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.error("参数校验异常：{}", msg);
        return Result.error(400, msg);
    }

    /** 算术异常（如除零等） */
    @ExceptionHandler(ArithmeticException.class)
    public Result<Void> handleArithmeticException(ArithmeticException e) {
        log.error("算术异常：{}", e.getMessage());
        return Result.error("系统计算异常");
    }

    /** 通用异常兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("系统繁忙，请稍后重试");
    }

}