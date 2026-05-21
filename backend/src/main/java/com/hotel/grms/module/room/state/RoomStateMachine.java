package com.hotel.grms.module.room.state;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客房占用态状态机；保洁态由 {@link RoomCleanStatus} 独立维护。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Component
public class RoomStateMachine {

    private static final Map<String, Set<String>> OCCUPANCY_TRANSITIONS = buildOccupancyTransitions();

    /**
     * 校验占用态迁移是否允许。
     *
     * @param fromStatus 当前占用态
     * @param toStatus   目标占用态
     */
    public void assertOccupancyTransition(String fromStatus, String toStatus) {
        String from = RoomStatus.normalizeOccupancy(fromStatus);
        String to = RoomStatus.normalizeOccupancy(toStatus);
        if (from.equals(to)) {
            return;
        }
        if (RoomStatus.OUT_OF_ORDER.equals(to)) {
            return;
        }
        Set<String> allowed = OCCUPANCY_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(40001, "占用态不允许从 " + from + " 变更为 " + to);
        }
    }

    /**
     * 校验是否允许入住（占用态空房/预订且保洁态为空净）。
     *
     * @param room 客房
     */
    public void assertCheckInAllowed(Room room) {
        if (room == null) {
            throw new BusinessException(40001, "客房不存在");
        }
        String occupancy = RoomStatus.normalizeOccupancy(room.getStatus());
        if (!RoomStatus.VACANT.equals(occupancy) && !RoomStatus.RESERVED.equals(occupancy)) {
            throw new BusinessException(40001, "当前占用态不可办理入住");
        }
        if (!RoomCleanStatus.CLEAN.equals(resolveCleanStatus(room))) {
            throw new BusinessException(40001, "脏房不可办理入住，请先置净");
        }
    }

    /**
     * 维修结束目标占用态。
     *
     * @param targetStatus 目标占用态
     */
    public void assertMaintenanceEndOccupancy(String targetStatus) {
        String target = RoomStatus.normalizeOccupancy(targetStatus);
        if (!RoomStatus.VACANT.equals(target)) {
            throw new BusinessException(40001, "维修结束占用态仅可为空房");
        }
    }

    /**
     * 维修结束目标保洁态。
     *
     * @param targetCleanStatus 目标保洁态
     */
    public void assertMaintenanceEndClean(String targetCleanStatus) {
        if (!RoomCleanStatus.CLEAN.equals(targetCleanStatus) && !RoomCleanStatus.DIRTY.equals(targetCleanStatus)) {
            throw new BusinessException(40001, "维修结束保洁态仅可为净房或脏房");
        }
    }

    private static String resolveCleanStatus(Room room) {
        if (room.getCleanStatus() != null) {
            return room.getCleanStatus();
        }
        if (RoomStatus.DIRTY.equals(room.getStatus())) {
            return RoomCleanStatus.DIRTY;
        }
        return RoomCleanStatus.CLEAN;
    }

    private static Map<String, Set<String>> buildOccupancyTransitions() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(RoomStatus.VACANT, setOf(RoomStatus.RESERVED, RoomStatus.OCCUPIED, RoomStatus.OUT_OF_ORDER));
        map.put(RoomStatus.RESERVED, setOf(RoomStatus.VACANT, RoomStatus.OCCUPIED));
        map.put(RoomStatus.OCCUPIED, setOf(RoomStatus.VACANT));
        map.put(RoomStatus.OUT_OF_ORDER, setOf(RoomStatus.VACANT));
        return map;
    }

    private static Set<String> setOf(String... values) {
        Set<String> set = new HashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
