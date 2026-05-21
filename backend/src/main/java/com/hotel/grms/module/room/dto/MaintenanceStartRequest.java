package com.hotel.grms.module.room.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 开始维修请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class MaintenanceStartRequest {

    @NotBlank(message = "维修原因不能为空")
    private String reason;
    @NotNull(message = "预计恢复时间不能为空")
    private LocalDateTime expectedRecoveryAt;
    private Integer version;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpectedRecoveryAt() {
        return expectedRecoveryAt;
    }

    public void setExpectedRecoveryAt(LocalDateTime expectedRecoveryAt) {
        this.expectedRecoveryAt = expectedRecoveryAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
