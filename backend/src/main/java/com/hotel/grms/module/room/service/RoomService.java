package com.hotel.grms.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.audit.support.AuditBizType;
import com.hotel.grms.module.audit.support.AuditContextHolder;
import com.hotel.grms.module.audit.support.AuditJsonHelper;
import com.hotel.grms.module.audit.support.AuditOpType;
import com.hotel.grms.module.audit.support.AuditedOperation;
import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.dto.ForceStatusRequest;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomStatusVersionRequest;
import com.hotel.grms.module.room.dto.RoomResponse;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.hk.service.HousekeepingService;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.room.state.RoomStateMachine;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客房维护与占用态/保洁态更新服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeService roomTypeService;
    private final RoomStateMachine roomStateMachine;
    private final HousekeepingService housekeepingService;
    private final AuditJsonHelper auditJsonHelper;

    public RoomService(RoomMapper roomMapper, RoomTypeService roomTypeService, RoomStateMachine roomStateMachine,
                       @Lazy HousekeepingService housekeepingService, AuditJsonHelper auditJsonHelper) {
        this.roomMapper = roomMapper;
        this.roomTypeService = roomTypeService;
        this.roomStateMachine = roomStateMachine;
        this.housekeepingService = housekeepingService;
        this.auditJsonHelper = auditJsonHelper;
    }

    /**
     * 查询全部楼层号（用于房态图筛选，不受当前筛选影响）。
     *
     * @return 楼层列表
     */
    public List<Integer> listFloors() {
        return roomMapper.selectDistinctFloors();
    }

    /**
     * 查询客房列表。
     *
     * @param floorNo 楼层，null 为全部
     * @return 客房列表
     */
    public List<RoomResponse> listRooms(Integer floorNo) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>().orderByAsc(Room::getFloorNo)
                .orderByAsc(Room::getRoomNo);
        if (floorNo != null) {
            wrapper.eq(Room::getFloorNo, floorNo);
        }
        List<Room> rooms = roomMapper.selectList(wrapper);
        Map<Long, String> typeNames = loadTypeNames(rooms);
        List<RoomResponse> result = new ArrayList<RoomResponse>(rooms.size());
        for (Room room : rooms) {
            result.add(toResponse(room, typeNames.get(room.getRoomTypeId())));
        }
        return result;
    }

    /**
     * 按 ID 查询客房。
     *
     * @param id 客房 ID
     * @return 客房实体
     */
    public Room getById(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException(40013, "客房不存在");
        }
        normalizeRoomFields(room);
        return room;
    }

    /**
     * 创建客房，初始为空房+空净。
     *
     * @param request 请求
     * @return 新客房
     */
    @Transactional(rollbackFor = Exception.class)
    public RoomResponse create(RoomRequest request) {
        assertRoomNoUnique(request.getRoomNo(), null);
        roomTypeService.getById(request.getRoomTypeId());
        Room room = new Room();
        room.setRoomNo(request.getRoomNo());
        room.setRoomTypeId(request.getRoomTypeId());
        room.setFloorNo(request.getFloorNo());
        room.setStatus(RoomStatus.VACANT);
        room.setCleanStatus(RoomCleanStatus.CLEAN);
        roomMapper.insert(room);
        RoomType type = roomTypeService.getById(room.getRoomTypeId());
        return toResponse(room, type.getName());
    }

    /**
     * 更新客房基础信息（不改房态）。
     *
     * @param id      客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = getById(id);
        assertRoomNoUnique(request.getRoomNo(), id);
        roomTypeService.getById(request.getRoomTypeId());
        room.setRoomNo(request.getRoomNo());
        room.setRoomTypeId(request.getRoomTypeId());
        room.setFloorNo(request.getFloorNo());
        updateWithOptimisticLock(room);
        RoomType type = roomTypeService.getById(room.getRoomTypeId());
        return toResponse(roomMapper.selectById(id), type.getName());
    }

    /**
     * 占用态迁移（校验状态机，不改保洁态）。
     *
     * @param roomId   客房 ID
     * @param toStatus 目标占用态
     * @param version  客户端版本号，可为 null
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room transitionOccupancy(Long roomId, String toStatus, Integer version) {
        Room room = getById(roomId);
        applyVersion(room, version);
        String target = RoomStatus.normalizeOccupancy(toStatus);
        roomStateMachine.assertOccupancyTransition(room.getStatus(), target);
        room.setStatus(target);
        updateWithOptimisticLock(room);
        return roomMapper.selectById(roomId);
    }

    /**
     * 设为脏房（仅保洁态，任意占用态均可）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room markDirty(Long roomId, RoomStatusVersionRequest request) {
        Room room = updateCleanStatus(roomId, RoomCleanStatus.DIRTY, request == null ? null : request.getVersion());
        housekeepingService.createTaskOnDirty(roomId);
        return room;
    }

    /**
     * 设为空净（仅保洁态）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room markClean(Long roomId, RoomStatusVersionRequest request) {
        return updateCleanStatus(roomId, RoomCleanStatus.CLEAN, request == null ? null : request.getVersion());
    }

    /**
     * 保洁态净/脏一键切换（与占用态无关，全部客房可用）。
     *
     * @param roomId 客房 ID
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room toggleCleanDirty(Long roomId) {
        Room room = getById(roomId);
        String next = RoomCleanStatus.DIRTY.equals(resolveCleanStatus(room))
                ? RoomCleanStatus.CLEAN : RoomCleanStatus.DIRTY;
        room.setCleanStatus(next);
        updateWithOptimisticLock(room);
        return roomMapper.selectById(roomId);
    }

    /**
     * 强制改占用态/保洁态（兼容历史目标码）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.ROOM, operationType = AuditOpType.ROOM_FORCE_STATUS)
    public Room forceStatus(Long roomId, ForceStatusRequest request) {
        Room room = getById(roomId);
        String beforeJson = auditJsonHelper.pairs("status", room.getStatus(), "cleanStatus", room.getCleanStatus());
        applyVersion(room, request.getVersion());
        applyForceTarget(room, request.getTargetStatus());
        updateWithOptimisticLock(room);
        Room updated = roomMapper.selectById(roomId);
        AuditContextHolder.bind(roomId, beforeJson,
                auditJsonHelper.pairs("status", updated.getStatus(), "cleanStatus", updated.getCleanStatus(),
                        "targetStatus", request.getTargetStatus(), "reason", request.getReason()),
                "强制改房态");
        return updated;
    }

    private Room updateCleanStatus(Long roomId, String cleanStatus, Integer version) {
        Room room = getById(roomId);
        applyVersion(room, version);
        room.setCleanStatus(cleanStatus);
        updateWithOptimisticLock(room);
        return roomMapper.selectById(roomId);
    }

    private void applyForceTarget(Room room, String targetStatus) {
        if (RoomCleanStatus.DIRTY.equals(targetStatus)) {
            room.setCleanStatus(RoomCleanStatus.DIRTY);
            return;
        }
        if (RoomCleanStatus.CLEAN.equals(targetStatus) || RoomStatus.VACANT_CLEAN.equals(targetStatus)) {
            room.setCleanStatus(RoomCleanStatus.CLEAN);
            if (RoomStatus.VACANT_CLEAN.equals(targetStatus)) {
                room.setStatus(RoomStatus.VACANT);
            }
            return;
        }
        room.setStatus(RoomStatus.normalizeOccupancy(targetStatus));
    }

    private void normalizeRoomFields(Room room) {
        room.setStatus(RoomStatus.normalizeOccupancy(room.getStatus()));
        if (room.getCleanStatus() == null || room.getCleanStatus().isEmpty()) {
            room.setCleanStatus(RoomCleanStatus.CLEAN);
        }
    }

    private String resolveCleanStatus(Room room) {
        if (room.getCleanStatus() != null) {
            return room.getCleanStatus();
        }
        return RoomCleanStatus.CLEAN;
    }

    private void assertRoomNoUnique(String roomNo, Long excludeId) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>().eq(Room::getRoomNo, roomNo);
        if (excludeId != null) {
            wrapper.ne(Room::getId, excludeId);
        }
        Long count = roomMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(40014, "房号已存在");
        }
    }

    private void applyVersion(Room room, Integer version) {
        if (version != null) {
            room.setVersion(version);
        }
    }

    private void updateWithOptimisticLock(Room room) {
        try {
            int updated = roomMapper.updateById(room);
            if (updated == 0) {
                throw new BusinessException(40901, "客房数据已被他人修改，请刷新后重试");
            }
        } catch (OptimisticLockingFailureException ex) {
            throw new BusinessException(40901, "客房数据已被他人修改，请刷新后重试");
        }
    }

    private Map<Long, String> loadTypeNames(List<Room> rooms) {
        Map<Long, String> map = new HashMap<Long, String>();
        for (Room room : rooms) {
            Long typeId = room.getRoomTypeId();
            if (typeId != null && !map.containsKey(typeId)) {
                map.put(typeId, roomTypeService.getById(typeId).getName());
            }
        }
        return map;
    }

    private RoomResponse toResponse(Room room, String typeName) {
        normalizeRoomFields(room);
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNo(room.getRoomNo());
        response.setRoomTypeId(room.getRoomTypeId());
        response.setRoomTypeName(typeName);
        response.setFloorNo(room.getFloorNo());
        response.setStatus(room.getStatus());
        response.setCleanStatus(room.getCleanStatus());
        response.setVersion(room.getVersion());
        return response;
    }
}
