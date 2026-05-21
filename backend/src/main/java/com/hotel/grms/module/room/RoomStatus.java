package com.hotel.grms.module.room;

/**
 * 客房主状态常量，与 plan §3.2 及 {@code room.status} 字段一致。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class RoomStatus {

    /** 空净，可入住 */
    public static final String VACANT_CLEAN = "VACANT_CLEAN";
    /** 预订锁定 */
    public static final String RESERVED = "RESERVED";
    /** 在住 */
    public static final String OCCUPIED = "OCCUPIED";
    /** 脏房 */
    public static final String DIRTY = "DIRTY";
    /** 维修 */
    public static final String OUT_OF_ORDER = "OUT_OF_ORDER";

    private RoomStatus() {
    }
}
