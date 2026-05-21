package com.hotel.grms.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 客房维修记录实体，记录原因与预计恢复时间。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@TableName("room_maintenance_log")
public class RoomMaintenanceLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String reason;
    private LocalDateTime expectedRecoveryAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long operatorId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}
