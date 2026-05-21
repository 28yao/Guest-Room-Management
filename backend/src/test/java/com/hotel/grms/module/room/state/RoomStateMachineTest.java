package com.hotel.grms.module.room.state;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.RoomStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 客房状态机单元测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
class RoomStateMachineTest {

    private final RoomStateMachine stateMachine = new RoomStateMachine();

    @Test
    void vacantCleanToReservedAllowed() {
        assertDoesNotThrow(() -> stateMachine.assertNormalTransition(
                RoomStatus.VACANT_CLEAN, RoomStatus.RESERVED));
    }

    @Test
    void occupiedToDirtyAllowed() {
        assertDoesNotThrow(() -> stateMachine.assertNormalTransition(
                RoomStatus.OCCUPIED, RoomStatus.DIRTY));
    }

    @Test
    void dirtyToVacantCleanAllowed() {
        assertDoesNotThrow(() -> stateMachine.assertNormalTransition(
                RoomStatus.DIRTY, RoomStatus.VACANT_CLEAN));
    }

    @Test
    void occupiedToVacantCleanRejected() {
        assertThrows(BusinessException.class, () -> stateMachine.assertNormalTransition(
                RoomStatus.OCCUPIED, RoomStatus.VACANT_CLEAN));
    }

    @Test
    void anyToOutOfOrderAllowed() {
        assertDoesNotThrow(() -> stateMachine.assertNormalTransition(
                RoomStatus.OCCUPIED, RoomStatus.OUT_OF_ORDER));
    }
}
