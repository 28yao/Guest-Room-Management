package com.hotel.grms.module.reservation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建预订请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class ReservationCreateRequest {

    @NotBlank(message = "客人姓名不能为空")
    private String guestName;

    @NotBlank(message = "联系电话不能为空")
    private String guestPhone;

    @NotNull(message = "房型不能为空")
    private Long roomTypeId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate arrivalDate;

    @NotNull(message = "离店日期不能为空")
    private LocalDate departureDate;

    /** 入住时刻，可选，默认入住日 18:00 */
    private LocalDateTime arrivalAt;

    /** 离店时刻，可选，默认离店日 12:00 */
    private LocalDateTime departureAt;

    private String remark;

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

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
