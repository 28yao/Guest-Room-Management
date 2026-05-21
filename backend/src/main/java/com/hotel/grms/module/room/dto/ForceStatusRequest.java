package com.hotel.grms.module.room.dto;

import javax.validation.constraints.NotBlank;

/**
 * 强制改房态请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class ForceStatusRequest {

    @NotBlank(message = "目标房态不能为空")
    private String targetStatus;
    @NotBlank(message = "改态原因不能为空")
    private String reason;
    private Integer version;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
