package com.hotel.grms.module.reservation.dto;

import javax.validation.constraints.NotNull;

/**
 * 预订预排房请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class AssignRoomRequest {

    @NotNull(message = "客房不能为空")
    private Long roomId;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
