package com.hotel.grms.module.stay.dto;

import javax.validation.constraints.NotNull;

/**
 * 在住换房请求。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class ChangeRoomRequest {

    @NotNull(message = "目标客房不能为空")
    private Long targetRoomId;
    private Integer targetRoomVersion;

    public Long getTargetRoomId() {
        return targetRoomId;
    }

    public void setTargetRoomId(Long targetRoomId) {
        this.targetRoomId = targetRoomId;
    }

    public Integer getTargetRoomVersion() {
        return targetRoomVersion;
    }

    public void setTargetRoomVersion(Integer targetRoomVersion) {
        this.targetRoomVersion = targetRoomVersion;
    }
}
