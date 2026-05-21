package com.hotel.grms.module.room.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房态图单项响应。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomBoardItemDto {

    private Long id;
    private String roomNo;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer floorNo;
    private String status;
    private String actualStatus;
    private Integer version;
    private BigDecimal rackRate;
    private List<String> dailyTags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(String actualStatus) {
        this.actualStatus = actualStatus;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public BigDecimal getRackRate() {
        return rackRate;
    }

    public void setRackRate(BigDecimal rackRate) {
        this.rackRate = rackRate;
    }

    public List<String> getDailyTags() {
        return dailyTags;
    }

    public void setDailyTags(List<String> dailyTags) {
        this.dailyTags = dailyTags;
    }
}
