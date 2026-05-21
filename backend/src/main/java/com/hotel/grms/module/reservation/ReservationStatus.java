package com.hotel.grms.module.reservation;

/**
 * 预订生命周期状态常量。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class ReservationStatus {

    /** 待确认 */
    public static final String PENDING = "PENDING";

    /** 已确认 */
    public static final String CONFIRMED = "CONFIRMED";

    /** 已入住 */
    public static final String CHECKED_IN = "CHECKED_IN";

    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";

    /** No-show */
    public static final String NO_SHOW = "NO_SHOW";

    /** 已释放 */
    public static final String RELEASED = "RELEASED";

    private ReservationStatus() {
    }
}
