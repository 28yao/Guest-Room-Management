package com.hotel.grms.module.room;

/**
 * 客房占用态常量，与 {@code room.status} 字段一致（与保洁态 {@link RoomCleanStatus} 独立）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class RoomStatus {

    /** 空房（无预订/在住占用） */
    public static final String VACANT = "VACANT";

    /** 预订锁定 */
    public static final String RESERVED = "RESERVED";

    /** 在住 */
    public static final String OCCUPIED = "OCCUPIED";

    /** 维修 */
    public static final String OUT_OF_ORDER = "OUT_OF_ORDER";

    /** @deprecated 迁移前占用态，等同 {@link #VACANT} */
    public static final String VACANT_CLEAN = "VACANT_CLEAN";

    /** @deprecated 迁移前保洁态，见 {@link RoomCleanStatus#DIRTY} */
    public static final String DIRTY = "DIRTY";

    private RoomStatus() {
    }

    /**
     * 将历史合并状态码规范为占用态。
     *
     * @param status 库内 status
     * @return 占用态
     */
    public static String normalizeOccupancy(String status) {
        if (VACANT_CLEAN.equals(status) || DIRTY.equals(status)) {
            return VACANT;
        }
        return status;
    }
}
