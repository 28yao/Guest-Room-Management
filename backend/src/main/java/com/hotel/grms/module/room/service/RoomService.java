package com.hotel.grms.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.dto.ForceStatusRequest;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomStatusVersionRequest;
import com.hotel.grms.module.room.dto.RoomResponse;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.room.state.RoomStateMachine;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客房维护与房态更新服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeService roomTypeService;
    private final RoomStateMachine roomStateMachine;

    public RoomService(RoomMapper roomMapper, RoomTypeService roomTypeService, RoomStateMachine roomStateMachine) {
        this.roomMapper = roomMapper;
        this.roomTypeService = roomTypeService;
        this.roomStateMachine = roomStateMachine;
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
        return room;
    }

    /**
     * 创建客房，初始状态为空净。
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
        room.setStatus(RoomStatus.VACANT_CLEAN);
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
     * 业务状态迁移（校验状态机）。
     *
     * @param roomId     客房 ID
     * @param toStatus   目标状态
     * @param version    客户端版本号，可为 null
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room transitionStatus(Long roomId, String toStatus, Integer version) {
        Room room = getById(roomId);
        applyVersion(room, version);
        roomStateMachine.assertNormalTransition(room.getStatus(), toStatus);
        room.setStatus(toStatus);
        updateWithOptimisticLock(room);
        return roomMapper.selectById(roomId);
    }

    /**
     * 设为脏房（走状态机校验）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room markDirty(Long roomId, RoomStatusVersionRequest request) {
        return transitionStatus(roomId, RoomStatus.DIRTY, request == null ? null : request.getVersion());
    }

    /**
     * 设为空净（走状态机校验，通常由保洁完成打扫）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room markClean(Long roomId, RoomStatusVersionRequest request) {
        return transitionStatus(roomId, RoomStatus.VACANT_CLEAN, request == null ? null : request.getVersion());
    }

    /**
     * 强制改房态（跳过状态机校验）。
     *
     * @param roomId  客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @Transactional(rollbackFor = Exception.class)
    public Room forceStatus(Long roomId, ForceStatusRequest request) {
        Room room = getById(roomId);
        applyVersion(room, request.getVersion());
        room.setStatus(request.getTargetStatus());
        updateWithOptimisticLock(room);
        return roomMapper.selectById(roomId);
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
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNo(room.getRoomNo());
        response.setRoomTypeId(room.getRoomTypeId());
        response.setRoomTypeName(typeName);
        response.setFloorNo(room.getFloorNo());
        response.setStatus(room.getStatus());
        response.setVersion(room.getVersion());
        return response;
    }
}
