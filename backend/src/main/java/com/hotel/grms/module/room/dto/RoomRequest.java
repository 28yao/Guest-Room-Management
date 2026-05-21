package com.hotel.grms.module.room.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 客房创建/更新请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomRequest {

    @NotBlank(message = "房号不能为空")
    private String roomNo;
    @NotNull(message = "房型不能为空")
    private Long roomTypeId;
    @NotNull(message = "楼层不能为空")
    private Integer floorNo;

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }
}
