package com.hotel.grms.module.room;

/**
 * 房态图当日叠加标签常量（非持久化主状态）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class DailyTag {

    /** 预抵：当日到店预订未入住 */
    public static final String EXPECTED_ARRIVAL = "EXPECTED_ARRIVAL";
    /** 预离：当日离店在住单 */
    public static final String EXPECTED_DEPARTURE = "EXPECTED_DEPARTURE";

    private DailyTag() {
    }
}
