package com.hotel.grms.module.room.state;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客房主状态机，校验合法迁移并判断可否入住。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Component
public class RoomStateMachine {

    private static final Map<String, Set<String>> NORMAL_TRANSITIONS = buildNormalTransitions();

    /**
     * 校验业务状态迁移是否允许（非强制改态）。
     *
     * @param fromStatus 当前状态
     * @param toStatus   目标状态
     */
    public void assertNormalTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw new BusinessException(40001, "房态不允许此操作");
        }
        if (fromStatus.equals(toStatus)) {
            return;
        }
        if (RoomStatus.OUT_OF_ORDER.equals(toStatus)) {
            return;
        }
        Set<String> allowed = NORMAL_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            throw new BusinessException(40001, "房态不允许从 " + fromStatus + " 变更为 " + toStatus);
        }
    }

    /**
     * 校验是否允许入住（空净或预订锁定且已排房由业务层保证）。
     *
     * @param room 客房
     */
    public void assertCheckInAllowed(Room room) {
        if (room == null) {
            throw new BusinessException(40001, "客房不存在");
        }
        String status = room.getStatus();
        if (!RoomStatus.VACANT_CLEAN.equals(status) && !RoomStatus.RESERVED.equals(status)) {
            throw new BusinessException(40001, "当前房态不可办理入住");
        }
    }

    /**
     * 校验维修结束目标状态。
     *
     * @param targetStatus 目标状态
     */
    public void assertMaintenanceEndTarget(String targetStatus) {
        if (!RoomStatus.DIRTY.equals(targetStatus) && !RoomStatus.VACANT_CLEAN.equals(targetStatus)) {
            throw new BusinessException(40001, "维修结束仅可恢复为脏房或空净");
        }
    }

    private static Map<String, Set<String>> buildNormalTransitions() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(RoomStatus.VACANT_CLEAN, setOf(RoomStatus.RESERVED, RoomStatus.OCCUPIED, RoomStatus.OUT_OF_ORDER));
        map.put(RoomStatus.RESERVED, setOf(RoomStatus.VACANT_CLEAN, RoomStatus.OCCUPIED));
        map.put(RoomStatus.OCCUPIED, setOf(RoomStatus.DIRTY));
        map.put(RoomStatus.DIRTY, setOf(RoomStatus.VACANT_CLEAN, RoomStatus.OUT_OF_ORDER));
        map.put(RoomStatus.OUT_OF_ORDER, setOf(RoomStatus.DIRTY, RoomStatus.VACANT_CLEAN));
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
