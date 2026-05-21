package com.hotel.grms.module.stay.dto;

import com.hotel.grms.module.billing.dto.CheckInPaymentItem;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 预订入住请求，可选调整房号。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class CheckInFromReservationRequest {

    @NotNull(message = "预订 ID 不能为空")
    private Long reservationId;
    private Long roomId;
    private BigDecimal agreedDailyRate;
    private String remark;

    @NotEmpty(message = "入住须登记收款")
    @Valid
    private List<CheckInPaymentItem> payments;

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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
