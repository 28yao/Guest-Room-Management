package com.hotel.grms.module.reservation.support;

import com.hotel.grms.module.stay.entity.StayOrder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预订/在住时刻与冲突判定单元测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
class ReservationTimePolicyTest {

    @Test
    void adjacentStaysDoNotConflict() {
        LocalDateTime firstStart = LocalDateTime.of(2026, 5, 23, 18, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2026, 5, 24, 12, 0);
        LocalDateTime secondStart = LocalDateTime.of(2026, 5, 24, 18, 0);
        LocalDateTime secondEnd = LocalDateTime.of(2026, 5, 25, 12, 0);
        assertFalse(ReservationTimePolicy.intervalsConflict(firstStart, firstEnd, secondStart, secondEnd));
    }

    @Test
    void overlappingStaysConflict() {
        LocalDateTime firstStart = LocalDateTime.of(2026, 5, 23, 18, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2026, 5, 25, 12, 0);
        LocalDateTime secondStart = LocalDateTime.of(2026, 5, 24, 18, 0);
        LocalDateTime secondEnd = LocalDateTime.of(2026, 5, 26, 12, 0);
        assertTrue(ReservationTimePolicy.intervalsConflict(firstStart, firstEnd, secondStart, secondEnd));
    }

    @Test
    void effectiveStayEndUsesDefaultDepartureTime() {
        StayOrder stay = new StayOrder();
        stay.setArrivalDate(LocalDate.of(2026, 5, 23));
        stay.setDepartureDate(LocalDate.of(2026, 5, 24));
        assertTrue(ReservationTimePolicy.effectiveStayEnd(stay).isEqual(LocalDateTime.of(2026, 5, 24, 12, 0)));
    }
}
