package com.hotel.grms.module.room.dto;

import java.math.BigDecimal;

/**
 * 房态图 SQL 行映射对象。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomBoardRowDto {

    private Long roomId;
    private String roomNo;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer floorNo;
    private String status;
    private String cleanStatus;
    private Integer version;
    private BigDecimal rackRate;
    private Integer expectedArrival;
    private Integer expectedDeparture;
    private Integer reservedOnViewDate;
    private Integer expectedResDeparture;
    private Integer inHouseOnViewDate;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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

    public Integer getExpectedArrival() {
        return expectedArrival;
    }

    public void setExpectedArrival(Integer expectedArrival) {
        this.expectedArrival = expectedArrival;
    }

    public Integer getExpectedDeparture() {
        return expectedDeparture;
    }

    public void setExpectedDeparture(Integer expectedDeparture) {
        this.expectedDeparture = expectedDeparture;
    }

    public Integer getReservedOnViewDate() {
        return reservedOnViewDate;
    }

    public void setReservedOnViewDate(Integer reservedOnViewDate) {
        this.reservedOnViewDate = reservedOnViewDate;
    }

    public Integer getExpectedResDeparture() {
        return expectedResDeparture;
    }

    public void setExpectedResDeparture(Integer expectedResDeparture) {
        this.expectedResDeparture = expectedResDeparture;
    }

    public Integer getInHouseOnViewDate() {
        return inHouseOnViewDate;
    }

    public void setInHouseOnViewDate(Integer inHouseOnViewDate) {
        this.inHouseOnViewDate = inHouseOnViewDate;
    }
}
