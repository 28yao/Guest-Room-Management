package com.hotel.grms.module.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.audit.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
