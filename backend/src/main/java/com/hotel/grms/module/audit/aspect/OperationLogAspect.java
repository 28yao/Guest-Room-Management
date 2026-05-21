package com.hotel.grms.module.audit.aspect;

import com.hotel.grms.module.audit.service.OperationLogService;
import com.hotel.grms.module.audit.support.AuditContextHolder;
import com.hotel.grms.module.audit.support.AuditedOperation;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 审计切面：在标注 {@link AuditedOperation} 的方法成功返回后落库。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 方法正常返回后写入审计。
     *
     * @param audited 注解
     */
    @AfterReturning("@annotation(audited)")
    public void afterSuccess(AuditedOperation audited) {
        AuditContextHolder.AuditPayload payload = AuditContextHolder.poll();
        if (payload == null || payload.getBizId() == null) {
            return;
        }
        operationLogService.save(audited.bizType(), payload.getBizId(), audited.operationType(),
                payload.getBeforeValue(), payload.getAfterValue(), payload.getSummary());
    }
}
