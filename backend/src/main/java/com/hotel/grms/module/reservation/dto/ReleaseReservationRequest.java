package com.hotel.grms.module.reservation.dto;

/**
 * 手动释放预订请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class ReleaseReservationRequest {

    /** 是否标记为 No-show */
    private Boolean noShow;

    public Boolean getNoShow() {
        return noShow;
    }

    public void setNoShow(Boolean noShow) {
        this.noShow = noShow;
    }
}
