package com.hotel.grms.module.room.dto;

/**
 * 房态变更请求体（含乐观锁版本号）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomStatusVersionRequest {

    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
