package com.hotel.grms.module.stay.dto;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

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
}
