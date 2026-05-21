package com.hotel.grms.module.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.common.PageResult;
import com.hotel.grms.module.audit.dto.OperationLogResponse;
import com.hotel.grms.module.audit.entity.OperationLog;
import com.hotel.grms.module.audit.mapper.OperationLogMapper;
import com.hotel.grms.security.LoginUser;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作审计日志写入与分页查询服务。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 写入一条审计日志。
     *
     * @param bizType       业务类型
     * @param bizId         业务 ID
     * @param operationType 操作类型
     * @param beforeValue   变更前 JSON
     * @param afterValue    变更后 JSON
     * @param summary       摘要
     */
    public void save(String bizType, Long bizId, String operationType,
                     String beforeValue, String afterValue, String summary) {
        if (bizId == null) {
            return;
        }
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setOperationType(operationType);
        log.setOperatorId(operatorId);
        log.setOperatorName(SecurityUtils.currentUsername());
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setSummary(summary);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    /**
     * 分页查询审计日志。
     *
     * @param page          页码
     * @param size          每页条数
     * @param bizType       业务类型筛选
     * @param operationType 操作类型筛选
     * @param fromDate      开始日（含）
     * @param toDate        结束日（含）
     * @return 分页结果
     */
    public PageResult<OperationLogResponse> pageQuery(int page, int size, String bizType, String operationType,
                                                      LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(40025, "查询结束日期不能早于开始日期");
        }
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>();
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(OperationLog::getBizType, bizType.trim());
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType.trim());
        }
        if (fromDate != null) {
            wrapper.ge(OperationLog::getCreatedAt, fromDate.atStartOfDay());
        }
        if (toDate != null) {
            wrapper.lt(OperationLog::getCreatedAt, toDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> pageQuery = new Page<OperationLog>(page, size);
        Page<OperationLog> result = operationLogMapper.selectPage(pageQuery, wrapper);
        List<OperationLogResponse> records = new ArrayList<OperationLogResponse>();
        for (OperationLog row : result.getRecords()) {
            records.add(toResponse(row));
        }
        return new PageResult<OperationLogResponse>(result.getTotal(), records);
    }

    private OperationLogResponse toResponse(OperationLog log) {
        OperationLogResponse response = new OperationLogResponse();
        response.setId(log.getId());
        response.setBizType(log.getBizType());
        response.setBizId(log.getBizId());
        response.setOperationType(log.getOperationType());
        response.setOperatorId(log.getOperatorId());
        response.setOperatorName(log.getOperatorName());
        response.setBeforeValue(log.getBeforeValue());
        response.setAfterValue(log.getAfterValue());
        response.setSummary(log.getSummary());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
