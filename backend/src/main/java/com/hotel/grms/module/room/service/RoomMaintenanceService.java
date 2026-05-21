package com.hotel.grms.module.room.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.dto.MaintenanceEndRequest;
import com.hotel.grms.module.room.dto.MaintenanceStartRequest;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomMaintenanceLog;
import com.hotel.grms.module.room.mapper.RoomMaintenanceLogMapper;
import com.hotel.grms.module.room.state.RoomStateMachine;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 客房维修开始/结束服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomMaintenanceService {

    private final RoomService roomService;
    private final RoomMaintenanceLogMapper maintenanceLogMapper;
    private final RoomStateMachine roomStateMachine;

    public RoomMaintenanceService(RoomService roomService,
                                  RoomMaintenanceLogMapper maintenanceLogMapper,
                                  RoomStateMachine roomStateMachine) {
        this.roomService = roomService;
        this.maintenanceLogMapper = maintenanceLogMapper;
        this.roomStateMachine = roomStateMachine;
    }

    /**
     * 开始维修，房态变为维修中。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room startMaintenance(Long roomId, MaintenanceStartRequest request) {
        if (!StringUtils.hasText(request.getReason())) {
            throw new BusinessException(40015, "维修原因不能为空");
        }
        if (request.getExpectedRecoveryAt() == null) {
            throw new BusinessException(40015, "预计恢复时间不能为空");
        }
        RoomMaintenanceLog open = maintenanceLogMapper.selectOpenByRoomId(roomId);
        if (open != null) {
            throw new BusinessException(40016, "该客房已在维修中");
        }
        Room room = roomService.transitionStatus(roomId, RoomStatus.OUT_OF_ORDER, request.getVersion());
        RoomMaintenanceLog log = new RoomMaintenanceLog();
        log.setRoomId(roomId);
        log.setReason(request.getReason());
        log.setExpectedRecoveryAt(request.getExpectedRecoveryAt());
        log.setStartedAt(LocalDateTime.now());
        log.setOperatorId(SecurityUtils.currentUserId());
        maintenanceLogMapper.insert(log);
        return room;
    }

    /**
     * 结束维修，恢复为脏房或空净。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room endMaintenance(Long roomId, MaintenanceEndRequest request) {
        roomStateMachine.assertMaintenanceEndTarget(request.getTargetStatus());
        RoomMaintenanceLog open = maintenanceLogMapper.selectOpenByRoomId(roomId);
        if (open == null) {
            throw new BusinessException(40017, "该客房无进行中的维修记录");
        }
        Room room = roomService.getById(roomId);
        if (!RoomStatus.OUT_OF_ORDER.equals(room.getStatus())) {
            throw new BusinessException(40001, "当前房态不是维修中");
        }
        room = roomService.transitionStatus(roomId, request.getTargetStatus(), request.getVersion());
        open.setEndedAt(LocalDateTime.now());
        maintenanceLogMapper.updateById(open);
        return room;
    }
}
