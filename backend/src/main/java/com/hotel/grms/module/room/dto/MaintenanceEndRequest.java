package com.hotel.grms.module.room.dto;

import javax.validation.constraints.NotBlank;

/**
 * 结束维修请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class MaintenanceEndRequest {

    @NotBlank(message = "恢复后房态不能为空")
    private String targetStatus;
    private Integer version;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
