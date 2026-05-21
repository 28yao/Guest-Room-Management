package com.hotel.grms.module.shift.dto;

/**
 * 结班请求体。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class ShiftCloseRequest {

    /** 有待办时是否强制结班（须 shift:force_close 权限） */
    private Boolean forceClose;

    public Boolean getForceClose() {
        return forceClose;
    }

    public void setForceClose(Boolean forceClose) {
        this.forceClose = forceClose;
    }
}
