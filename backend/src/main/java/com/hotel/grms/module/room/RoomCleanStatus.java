package com.hotel.grms.module.room;

/**
 * 客房保洁态常量，与 {@code room.clean_status} 字段一致。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class RoomCleanStatus {

    /** 空净（可售保洁完成） */
    public static final String CLEAN = "CLEAN";

    /** 脏房（待打扫） */
    public static final String DIRTY = "DIRTY";

    private RoomCleanStatus() {
    }
}
