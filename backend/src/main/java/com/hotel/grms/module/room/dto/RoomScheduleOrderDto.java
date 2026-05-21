package com.hotel.grms.module.room.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房态图客房日程中的单条订单（预订或在住）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomScheduleOrderDto {

    /** 订单类型：RESERVATION / STAY */
    private String orderType;
    private Long orderId;
    private String orderNo;
    private String guestName;
    private String guestPhone;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private LocalDateTime arrivalAt;
    private LocalDateTime departureAt;
    private String status;
    private String remark;
    private BigDecimal agreedDailyRate;
    /** 是否允许在前端编辑（预订可改；在住可改备注） */
    private Boolean editable;

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(LocalDate arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public void setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public void setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getAgreedDailyRate() {
        return agreedDailyRate;
    }

    public void setAgreedDailyRate(BigDecimal agreedDailyRate) {
        this.agreedDailyRate = agreedDailyRate;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }
}
