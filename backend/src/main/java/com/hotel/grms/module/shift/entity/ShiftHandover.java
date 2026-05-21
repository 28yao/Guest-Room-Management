package com.hotel.grms.module.shift.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结班快照实体，对应 shift_handover 表。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@TableName("shift_handover")
public class ShiftHandover {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shiftSessionId;
    private BigDecimal cashTotal;
    private BigDecimal wechatTotal;
    private BigDecimal alipayTotal;
    private String pendingSnapshot;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShiftSessionId() {
        return shiftSessionId;
    }

    public void setShiftSessionId(Long shiftSessionId) {
        this.shiftSessionId = shiftSessionId;
    }

    public BigDecimal getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(BigDecimal cashTotal) {
        this.cashTotal = cashTotal;
    }

    public BigDecimal getWechatTotal() {
        return wechatTotal;
    }

    public void setWechatTotal(BigDecimal wechatTotal) {
        this.wechatTotal = wechatTotal;
    }

    public BigDecimal getAlipayTotal() {
        return alipayTotal;
    }

    public void setAlipayTotal(BigDecimal alipayTotal) {
        this.alipayTotal = alipayTotal;
    }

    public String getPendingSnapshot() {
        return pendingSnapshot;
    }

    public void setPendingSnapshot(String pendingSnapshot) {
        this.pendingSnapshot = pendingSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
