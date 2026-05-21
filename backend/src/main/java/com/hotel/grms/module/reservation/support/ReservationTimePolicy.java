package com.hotel.grms.module.reservation.support;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.stay.entity.StayOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预订入住/离店时刻默认值与区间校验（BR-12、打扫缓冲）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class ReservationTimePolicy {

    /** 默认入住时刻：当天 18:00 */
    public static final LocalTime DEFAULT_ARRIVAL_TIME = LocalTime.of(18, 0);

    /** 默认离店时刻：离店日 12:00 */
    public static final LocalTime DEFAULT_DEPARTURE_TIME = LocalTime.of(12, 0);

    /** 相邻占用最小打扫间隔（小时） */
    public static final int CLEANING_BUFFER_HOURS = 1;

    private ReservationTimePolicy() {
    }

    /**
     * 根据日期与可选时刻解析完整入住/离店时间。
     *
     * @param arrivalDate   入住日
     * @param departureDate 离店日
     * @param arrivalAt     入住时刻，可为 null
     * @param departureAt   离店时刻，可为 null
     * @return 长度为 2 的数组：[0]=arrivalAt,[1]=departureAt
     */
    public static LocalDateTime[] resolveRange(LocalDate arrivalDate, LocalDate departureDate,
                                               LocalDateTime arrivalAt, LocalDateTime departureAt) {
        if (arrivalDate == null || departureDate == null) {
            throw new BusinessException(40015, "入住日与离店日不能为空");
        }
        LocalDateTime start = arrivalAt != null
                ? arrivalAt
                : LocalDateTime.of(arrivalDate, DEFAULT_ARRIVAL_TIME);
        LocalDateTime end = departureAt != null
                ? departureAt
                : LocalDateTime.of(departureDate, DEFAULT_DEPARTURE_TIME);
        assertValidRange(start, end);
        return new LocalDateTime[]{start, end};
    }

    /**
     * 校验离店时刻晚于入住时刻。
     *
     * @param arrivalAt   入住时刻
     * @param departureAt 离店时刻
     */
    public static void assertValidRange(LocalDateTime arrivalAt, LocalDateTime departureAt) {
        if (arrivalAt == null || departureAt == null || !departureAt.isAfter(arrivalAt)) {
            throw new BusinessException(40015, "离店时刻须晚于入住时刻");
        }
    }

    /**
     * 判断两段占用是否冲突（含 1 小时打扫缓冲）。
     *
     * @param aStart 段 A 入住
     * @param aEnd   段 A 离店
     * @param bStart 段 B 入住
     * @param bEnd   段 B 离店
     * @return 是否冲突
     */
    public static boolean intervalsConflict(LocalDateTime aStart, LocalDateTime aEnd,
                                          LocalDateTime bStart, LocalDateTime bEnd) {
        LocalDateTime aEndBuffered = aEnd.plusHours(CLEANING_BUFFER_HOURS);
        LocalDateTime bEndBuffered = bEnd.plusHours(CLEANING_BUFFER_HOURS);
        return aStart.isBefore(bEndBuffered) && bStart.isBefore(aEndBuffered);
    }

    /**
     * 判断查看日（自然日）是否与预订占用区间相交。
     *
     * @param viewDate    查看日
     * @param arrivalAt   入住时刻
     * @param departureAt 离店时刻
     * @return 是否占用
     */
    public static boolean occupiesViewDate(LocalDate viewDate, LocalDateTime arrivalAt, LocalDateTime departureAt) {
        LocalDateTime dayStart = viewDate.atStartOfDay();
        LocalDateTime dayEnd = viewDate.plusDays(1).atStartOfDay();
        return arrivalAt.isBefore(dayEnd) && departureAt.isAfter(dayStart);
    }

    /**
     * 在住单有效入住时刻（已入住用 check_in_at，否则默认 18:00）。
     *
     * @param stay 在住单
     * @return 入住时刻
     */
    public static LocalDateTime effectiveStayStart(StayOrder stay) {
        if (stay.getCheckInAt() != null) {
            return stay.getCheckInAt();
        }
        return LocalDateTime.of(stay.getArrivalDate(), DEFAULT_ARRIVAL_TIME);
    }

    /**
     * 在住单有效离店时刻（离店日默认 12:00）。
     *
     * @param stay 在住单
     * @return 离店时刻
     */
    public static LocalDateTime effectiveStayEnd(StayOrder stay) {
        return LocalDateTime.of(stay.getDepartureDate(), DEFAULT_DEPARTURE_TIME);
    }
}
