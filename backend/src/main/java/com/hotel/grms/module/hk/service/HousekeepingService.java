package com.hotel.grms.module.hk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.hk.HkTaskStatus;
import com.hotel.grms.module.hk.dto.HkTaskResponse;
import com.hotel.grms.module.hk.entity.HkTask;
import com.hotel.grms.module.hk.mapper.HkTaskMapper;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.room.service.RoomService;
import com.hotel.grms.module.room.service.RoomTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保洁任务服务：脏房创建任务、待扫列表、完成置空净。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class HousekeepingService {

    private final HkTaskMapper hkTaskMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public HousekeepingService(HkTaskMapper hkTaskMapper, RoomMapper roomMapper,
                              RoomService roomService, RoomTypeService roomTypeService) {
        this.hkTaskMapper = hkTaskMapper;
        this.roomMapper = roomMapper;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    /**
     * 为客房创建待打扫任务（若已有 PENDING 则跳过）。
     *
     * @param roomId 客房 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTaskOnDirty(Long roomId) {
        Long pending = hkTaskMapper.selectCount(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getRoomId, roomId)
                .eq(HkTask::getStatus, HkTaskStatus.PENDING));
        if (pending != null && pending > 0) {
            return;
        }
        HkTask task = new HkTask();
        task.setRoomId(roomId);
        task.setStatus(HkTaskStatus.PENDING);
        hkTaskMapper.insert(task);
    }

    /**
     * 查询保洁任务列表（默认待打扫）。
     *
     * @param floorNo 可选楼层
     * @param status  任务状态
     * @return 任务视图列表
     */
    public List<HkTaskResponse> listTasks(Integer floorNo, String status) {
        String taskStatus = status == null || status.isEmpty() ? HkTaskStatus.PENDING : status;
        List<HkTask> tasks = hkTaskMapper.selectList(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getStatus, taskStatus)
                .orderByAsc(HkTask::getCreatedAt));
        if (tasks.isEmpty()) {
            return new ArrayList<HkTaskResponse>();
        }
        List<Long> roomIds = collectRoomIds(tasks);
        Map<Long, Room> roomMap = loadRoomMap(roomIds);
        Map<Long, String> typeNameMap = loadRoomTypeNames(roomMap);
        List<HkTaskResponse> result = new ArrayList<HkTaskResponse>();
        for (HkTask task : tasks) {
            Room room = roomMap.get(task.getRoomId());
            if (room == null) {
                continue;
            }
            if (floorNo != null && !floorNo.equals(room.getFloorNo())) {
                continue;
            }
            result.add(toResponse(task, room, typeNameMap.get(room.getRoomTypeId())));
        }
        return result;
    }

    /**
     * 完成保洁：关闭任务并将客房保洁态置净（占用态须为空房）。
     *
     * @param taskId 任务 ID
     * @return 已完成任务
     */
    @Transactional(rollbackFor = Exception.class)
    public HkTaskResponse completeTask(Long taskId) {
        HkTask task = hkTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(40017, "保洁任务不存在");
        }
        if (HkTaskStatus.COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(40901, "保洁任务已完成");
        }
        if (!HkTaskStatus.PENDING.equals(task.getStatus())) {
            throw new BusinessException(40001, "任务状态不可完成");
        }
        Room room = roomService.getById(task.getRoomId());
        String occupancy = RoomStatus.normalizeOccupancy(room.getStatus());
        if (RoomStatus.OCCUPIED.equals(occupancy) || RoomStatus.RESERVED.equals(occupancy)) {
            throw new BusinessException(40001, "客房仍在占用，无法完成保洁");
        }
        roomService.markClean(task.getRoomId(), null);
        task.setStatus(HkTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        hkTaskMapper.updateById(task);
        Room updated = roomService.getById(task.getRoomId());
        RoomType type = roomTypeService.getById(updated.getRoomTypeId());
        String typeName = type != null ? type.getName() : "";
        return toResponse(task, updated, typeName);
    }

    private List<Long> collectRoomIds(List<HkTask> tasks) {
        List<Long> roomIds = new ArrayList<Long>();
        for (HkTask task : tasks) {
            if (task.getRoomId() != null && !roomIds.contains(task.getRoomId())) {
                roomIds.add(task.getRoomId());
            }
        }
        return roomIds;
    }

    private Map<Long, Room> loadRoomMap(List<Long> roomIds) {
        List<Room> rooms = roomMapper.selectBatchIds(roomIds);
        Map<Long, Room> map = new HashMap<Long, Room>();
        for (Room room : rooms) {
            map.put(room.getId(), room);
        }
        return map;
    }

    private Map<Long, String> loadRoomTypeNames(Map<Long, Room> roomMap) {
        Map<Long, String> names = new HashMap<Long, String>();
        for (Room room : roomMap.values()) {
            Long typeId = room.getRoomTypeId();
            if (typeId == null || names.containsKey(typeId)) {
                continue;
            }
            RoomType type = roomTypeService.getById(typeId);
            names.put(typeId, type != null ? type.getName() : "");
        }
        return names;
    }

    private HkTaskResponse toResponse(HkTask task, Room room, String roomTypeName) {
        HkTaskResponse dto = new HkTaskResponse();
        dto.setId(task.getId());
        dto.setRoomId(task.getRoomId());
        dto.setRoomNo(room.getRoomNo());
        dto.setFloorNo(room.getFloorNo());
        dto.setRoomTypeName(roomTypeName);
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setCompletedAt(task.getCompletedAt());
        return dto;
    }
}
