package com.hotel.grms.module.shift.dto;

import java.time.LocalDateTime;

/**
 * 当前开班会话响应。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class ShiftSessionResponse {

    private Long id;
    private Long operatorId;
    private LocalDateTime openedAt;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
