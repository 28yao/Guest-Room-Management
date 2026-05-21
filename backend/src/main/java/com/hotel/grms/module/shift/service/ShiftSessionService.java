package com.hotel.grms.module.shift.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.shift.ShiftSessionStatus;
import com.hotel.grms.module.shift.dto.ShiftSessionResponse;
import com.hotel.grms.module.shift.entity.ShiftSession;
import com.hotel.grms.module.shift.mapper.ShiftSessionMapper;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 开班会话服务：开班、查询当前班、校验是否已开班。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class ShiftSessionService {

    private final ShiftSessionMapper shiftSessionMapper;

    public ShiftSessionService(ShiftSessionMapper shiftSessionMapper) {
        this.shiftSessionMapper = shiftSessionMapper;
    }

    /**
     * 为当前操作员开班；若已有 OPEN 班则直接返回。
     *
     * @return 开班会话
     */
    @Transactional(rollbackFor = Exception.class)
    public ShiftSessionResponse openCurrent() {
        Long operatorId = requireOperatorId();
        ShiftSession existing = findOpenSession(operatorId);
        if (existing != null) {
            return toResponse(existing);
        }
        ShiftSession session = new ShiftSession();
        session.setOperatorId(operatorId);
        session.setOpenedAt(LocalDateTime.now());
        session.setStatus(ShiftSessionStatus.OPEN);
        shiftSessionMapper.insert(session);
        return toResponse(session);
    }

    /**
     * 查询当前操作员 OPEN 班，无则返回 null。
     *
     * @return 会话或 null
     */
    public ShiftSessionResponse getCurrent() {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            return null;
        }
        ShiftSession session = findOpenSession(operatorId);
        return session == null ? null : toResponse(session);
    }

    /**
     * 要求当前操作员已开班，否则抛 40003。
     *
     * @return 开班会话 ID
     */
    public Long requireOpenSessionId() {
        Long operatorId = requireOperatorId();
        ShiftSession session = findOpenSession(operatorId);
        if (session == null) {
            throw new BusinessException(40003, "请先开班后再办理入住或收款");
        }
        return session.getId();
    }

    private ShiftSession findOpenSession(Long operatorId) {
        return shiftSessionMapper.selectOne(new LambdaQueryWrapper<ShiftSession>()
                .eq(ShiftSession::getOperatorId, operatorId)
                .eq(ShiftSession::getStatus, ShiftSessionStatus.OPEN)
                .last("LIMIT 1"));
    }

    private Long requireOperatorId() {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            throw new BusinessException(40101, "未登录");
        }
        return operatorId;
    }

    private ShiftSessionResponse toResponse(ShiftSession session) {
        ShiftSessionResponse response = new ShiftSessionResponse();
        response.setId(session.getId());
        response.setOperatorId(session.getOperatorId());
        response.setOpenedAt(session.getOpenedAt());
        response.setStatus(session.getStatus());
        return response;
    }
}
