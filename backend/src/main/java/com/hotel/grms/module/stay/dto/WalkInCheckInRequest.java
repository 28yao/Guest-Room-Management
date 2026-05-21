package com.hotel.grms.module.stay.dto;

import com.hotel.grms.module.billing.dto.CheckInPaymentItem;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Walk-in 入住请求。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class WalkInCheckInRequest {

    @NotNull(message = "客房不能为空")
    private Long roomId;
    @NotBlank(message = "客人姓名不能为空")
    private String guestName;
    @NotBlank(message = "联系电话不能为空")
    private String guestPhone;
    private String idCard;
    @NotNull(message = "入住日期不能为空")
    private LocalDate arrivalDate;
    @NotNull(message = "离店日期不能为空")
    private LocalDate departureDate;
    /** 入住时刻，未传时默认入住日 18:00 */
    private LocalDateTime arrivalAt;
    /** 离店时刻，未传时默认离店日 12:00 */
    private LocalDateTime departureAt;
    private BigDecimal agreedDailyRate;
    private String remark;

    @NotEmpty(message = "入住须登记收款")
    @Valid
    private List<CheckInPaymentItem> payments;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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

    public BigDecimal getAgreedDailyRate() {
        return agreedDailyRate;
    }

    public void setAgreedDailyRate(BigDecimal agreedDailyRate) {
        this.agreedDailyRate = agreedDailyRate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<CheckInPaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<CheckInPaymentItem> payments) {
        this.payments = payments;
    }
}
