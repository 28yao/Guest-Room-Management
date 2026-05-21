package com.hotel.grms.module.audit.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需在方法成功后写入操作审计（配合 {@link AuditContextHolder}）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedOperation {

    /**
     * 业务类型。
     *
     * @return 类型码
     */
    String bizType();

    /**
     * 操作类型。
     *
     * @return 操作码
     */
    String operationType();
}
