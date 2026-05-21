package com.hotel.grms.module.audit.controller;

import com.hotel.grms.common.PageResult;
import com.hotel.grms.common.R;
import com.hotel.grms.module.audit.dto.OperationLogResponse;
import com.hotel.grms.module.audit.service.OperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 操作审计查询 REST 接口。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final OperationLogService operationLogService;

    public AuditController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志。
     *
     * @param page          页码，从 1 开始
     * @param size          每页条数
     * @param bizType       业务类型
     * @param operationType 操作类型
     * @param from          开始日期
     * @param to            结束日期
     * @return 分页列表
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('audit:view')")
    public R<PageResult<OperationLogResponse>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(operationLogService.pageQuery(page, size, bizType, operationType, from, to));
    }
}
