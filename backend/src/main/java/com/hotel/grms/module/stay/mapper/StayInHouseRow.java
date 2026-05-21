package com.hotel.grms.module.stay.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 在住列表查询结果行（Mapper 投影）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class StayInHouseRow {

    private Long stayId;
    private String stayNo;
    private Long reservationId;
    private String resNo;
    private Long roomId;
    private String roomNo;
    private Integer roomVersion;
    private Long roomTypeId;
    private String roomTypeName;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal agreedDailyRate;
    private String status;
    private String remark;
    private LocalDateTime checkInAt;
    private String guestName;
    private String guestPhone;
    private String idCard;
    private Long folioId;
    private BigDecimal folioTotalAmount;

    public Long getStayId() {
        return stayId;
    }

    public void setStayId(Long stayId) {
        this.stayId = stayId;
    }

    public String getStayNo() {
        return stayNo;
    }

    public void setStayNo(String stayNo) {
        this.stayNo = stayNo;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getResNo() {
        return resNo;
    }

    public void setResNo(String resNo) {
        this.resNo = resNo;
    }

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

    public Integer getRoomVersion() {
        return roomVersion;
    }

    public void setRoomVersion(Integer roomVersion) {
        this.roomVersion = roomVersion;
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

    public BigDecimal getAgreedDailyRate() {
        return agreedDailyRate;
    }

    public void setAgreedDailyRate(BigDecimal agreedDailyRate) {
        this.agreedDailyRate = agreedDailyRate;
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

    public LocalDateTime getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(LocalDateTime checkInAt) {
        this.checkInAt = checkInAt;
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

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Long getFolioId() {
        return folioId;
    }

    public void setFolioId(Long folioId) {
        this.folioId = folioId;
    }

    public BigDecimal getFolioTotalAmount() {
        return folioTotalAmount;
    }

    public void setFolioTotalAmount(BigDecimal folioTotalAmount) {
        this.folioTotalAmount = folioTotalAmount;
    }
}
