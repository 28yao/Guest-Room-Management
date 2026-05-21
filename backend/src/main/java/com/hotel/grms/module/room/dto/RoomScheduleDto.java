package com.hotel.grms.module.room.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 房态图点击客房后的日程与快速办理上下文。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomScheduleDto {

    private Long roomId;
    private String roomNo;
    private Long roomTypeId;
    private String roomTypeName;
    private BigDecimal rackRate;
    /** 库内占用态 */
    private String occupancyStatus;
    /** 库内保洁态 */
    private String cleanStatus;
    private Integer version;
    private LocalDate viewDate;
    /** 查看日是否已有预订或在住占用 */
    private Boolean occupiedOnViewDate;
    private List<RoomScheduleOrderDto> orders = new ArrayList<RoomScheduleOrderDto>();

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

    public BigDecimal getRackRate() {
        return rackRate;
    }

    public void setRackRate(BigDecimal rackRate) {
        this.rackRate = rackRate;
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

    public LocalDate getViewDate() {
        return viewDate;
    }

    public void setViewDate(LocalDate viewDate) {
        this.viewDate = viewDate;
    }

    public Boolean getOccupiedOnViewDate() {
        return occupiedOnViewDate;
    }

    public void setOccupiedOnViewDate(Boolean occupiedOnViewDate) {
        this.occupiedOnViewDate = occupiedOnViewDate;
    }

    public List<RoomScheduleOrderDto> getOrders() {
        return orders;
    }

    public void setOrders(List<RoomScheduleOrderDto> orders) {
        this.orders = orders;
    }
}
