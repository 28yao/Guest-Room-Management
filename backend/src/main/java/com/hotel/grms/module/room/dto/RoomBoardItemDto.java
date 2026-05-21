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
    /** 查看日占用展示态（在住/预订/空房/维修） */
    private String status;
    /** 库内占用态 */
    private String occupancyStatus;
    /** 库内保洁态 */
    private String cleanStatus;
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

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public String getCleanStatus() {
        return cleanStatus;
    }

    public void setCleanStatus(String cleanStatus) {
        this.cleanStatus = cleanStatus;
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
