package com.hotel.grms.module.room.state;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 占用态状态机单元测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
class RoomStateMachineTest {

    private final RoomStateMachine machine = new RoomStateMachine();

    @Test
    void vacantToReservedAllowed() {
        assertDoesNotThrow(() -> machine.assertOccupancyTransition(
                RoomStatus.VACANT, RoomStatus.RESERVED));
    }

    @Test
    void vacantToOccupiedAllowed() {
        assertDoesNotThrow(() -> machine.assertOccupancyTransition(
                RoomStatus.VACANT, RoomStatus.OCCUPIED));
    }

    @Test
    void occupiedToVacantAllowed() {
        assertDoesNotThrow(() -> machine.assertOccupancyTransition(
                RoomStatus.OCCUPIED, RoomStatus.VACANT));
    }

    @Test
    void occupiedToReservedRejected() {
        assertThrows(BusinessException.class, () -> machine.assertOccupancyTransition(
                RoomStatus.OCCUPIED, RoomStatus.RESERVED));
    }

    @Test
    void checkInAllowedWhenVacantClean() {
        Room room = room(RoomStatus.VACANT, RoomCleanStatus.CLEAN);
        assertDoesNotThrow(() -> machine.assertCheckInAllowed(room));
    }

    @Test
    void checkInRejectedWhenDirty() {
        Room room = room(RoomStatus.VACANT, RoomCleanStatus.DIRTY);
        assertThrows(BusinessException.class, () -> machine.assertCheckInAllowed(room));
    }

    @Test
    void checkInAllowedWhenReservedClean() {
        Room room = room(RoomStatus.RESERVED, RoomCleanStatus.CLEAN);
        assertDoesNotThrow(() -> machine.assertCheckInAllowed(room));
    }

    private Room room(String occupancy, String clean) {
        Room room = new Room();
        room.setStatus(occupancy);
        room.setCleanStatus(clean);
        return room;
    }
}
