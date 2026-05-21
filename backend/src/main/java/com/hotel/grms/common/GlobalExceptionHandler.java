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

    private static final String SCHEMA_MISMATCH_MIGRATION_HINT =
            "数据库表结构与当前版本不一致，请在 grms 库依次执行："
                    + "sql/V3__auth_add_description.sql（缺 description 列）、"
                    + "sql/V5__schema_align_legacy.sql（缺 room/room_type 等列）、"
                    + "sql/V7__reservation_datetime.sql（缺 arrival_at/departure_at 列）、"
                    + "sql/V8__stay_order_guest.sql（缺 stay_order.guest_name 列）、"
                    + "sql/V9__folio_line_billing.sql（缺 folio_line.quantity/unit_price 列）、"
                    + "sql/V10__folio_timestamps.sql（缺 folio.created_at/updated_at 列），"
                    + "或重新执行 sql/V1、V2 全量建库";

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
    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleCannotConnect(CannotGetJdbcConnectionException ex) {
        LOGGER.error("数据库连接失败", ex);
        return R.fail(50001, "数据库连接失败，请检查 MySQL 是否启动、库表是否已初始化、账号密码是否正确");
    }

    /**
     * 处理 SQL 执行失败（非连接池不可用）。
     *
     * @param ex 数据访问异常
     * @return 失败响应
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleDataAccess(DataAccessException ex) {
        LOGGER.error("数据库访问失败", ex);
        return R.fail(50002, resolveDataAccessMessage(ex));
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
        return R.fail(50002, resolveDataAccessMessage(ex));
    }

    private String resolveDataAccessMessage(Throwable ex) {
        String schemaHint = resolveSchemaMismatchHint(ex);
        if (schemaHint != null) {
            return schemaHint;
        }
        String constraintHint = resolveSqlConstraintHint(ex);
        if (constraintHint != null) {
            return constraintHint;
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root.getMessage() != null) {
            return "数据库操作失败：" + root.getMessage();
        }
        return "数据库访问失败，请检查表结构与后端日志";
    }

    private String resolveSqlConstraintHint(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                if (message.contains("guest_name") && message.contains("doesn't have a default value")) {
                    return "在住单表缺少客人姓名写入，请执行 sql/V8__stay_order_guest.sql 对齐表结构后重启后端";
                }
                if (message.contains("doesn't have a default value") || message.contains("cannot be null")) {
                    return "数据库字段约束不满足，请核对表结构与迁移脚本（V3/V5/V7/V8/V9/V10）";
                }
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    private String resolveSchemaMismatchHint(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("Unknown column")
                    || message.contains("doesn't exist"))) {
                if (message.contains("quantity") || message.contains("unit_price")) {
                    return "账单明细表结构与当前版本不一致，请在 grms 库执行 sql/V9__folio_line_billing.sql（缺 quantity/unit_price 列），或重新执行 sql/V1、V2 全量建库";
                }
                if (message.contains("created_at") || message.contains("updated_at")) {
                    return "账单主表结构与当前版本不一致，请在 grms 库执行 sql/V10__folio_timestamps.sql（缺 folio.created_at/updated_at 列；换房重算常见），或重新执行 sql/V1、V2 全量建库";
                }
                return SCHEMA_MISMATCH_MIGRATION_HINT;
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
