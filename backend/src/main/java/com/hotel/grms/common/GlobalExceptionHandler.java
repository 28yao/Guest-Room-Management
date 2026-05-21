package com.hotel.grms.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一 API 错误响应格式。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusiness(BusinessException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理认证失败。
     *
     * @param ex 认证异常
     * @return 失败响应
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuth(AuthenticationException ex) {
        return R.fail(40100, ex.getMessage() != null ? ex.getMessage() : "认证失败");
    }

    /**
     * 处理权限不足。
     *
     * @param ex 访问拒绝异常
     * @return 失败响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDenied(AccessDeniedException ex) {
        return R.fail(40300, "无操作权限");
    }

    /**
     * 处理数据库连接失败。
     *
     * @param ex 连接异常
     * @return 失败响应
     */
    @ExceptionHandler({CannotGetJdbcConnectionException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleDataAccess(DataAccessException ex) {
        LOGGER.error("数据库访问失败", ex);
        String schemaHint = resolveSchemaMismatchHint(ex);
        if (schemaHint != null) {
            return R.fail(50002, schemaHint);
        }
        return R.fail(50001, "数据库连接失败，请检查 MySQL 是否启动、库表是否已初始化、账号密码是否正确");
    }

    /**
     * 处理 MyBatis 包装的数据库异常。
     *
     * @param ex MyBatis 异常
     * @return 失败响应
     */
    @ExceptionHandler(MyBatisSystemException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleMyBatis(MyBatisSystemException ex) {
        LOGGER.error("MyBatis 执行失败", ex);
        Throwable root = ex.getCause();
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != null && root.getMessage() != null && root.getMessage().contains("Access denied")) {
            return R.fail(50001, "数据库连接失败，请检查 MySQL 账号密码（可复制 application-local.yml.example）");
        }
        String schemaHint = resolveSchemaMismatchHint(ex);
        if (schemaHint != null) {
            return R.fail(50002, schemaHint);
        }
        return R.fail(50001, "数据库访问失败，请检查数据库配置与表结构");
    }

    private String resolveSchemaMismatchHint(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("Unknown column")
                    || message.contains("doesn't exist"))) {
                return "数据库表结构与当前版本不一致，请在 grms 库执行 sql/V3__auth_add_description.sql 或重新执行 sql/V1、V2 初始化脚本";
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    /**
     * 处理未捕获异常。
     *
     * @param ex 异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleUnknown(Exception ex) {
        LOGGER.error("系统异常", ex);
        return R.fail(50000, "系统繁忙，请稍后重试");
    }
}
