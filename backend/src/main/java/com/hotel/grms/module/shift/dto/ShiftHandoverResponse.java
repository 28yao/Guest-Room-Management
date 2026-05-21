package com.hotel.grms.module.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结班单详情响应。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class ShiftHandoverResponse {

    private Long handoverId;
    private Long shiftSessionId;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal cashTotal;
    private BigDecimal wechatTotal;
    private BigDecimal alipayTotal;
    private List<HandoverPendingItem> pendingItems;

    public Long getHandoverId() {
        return handoverId;
    }

    public void setHandoverId(Long handoverId) {
        this.handoverId = handoverId;
    }

    public Long getShiftSessionId() {
        return shiftSessionId;
    }

    public void setShiftSessionId(Long shiftSessionId) {
        this.shiftSessionId = shiftSessionId;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
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

    public List<HandoverPendingItem> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<HandoverPendingItem> pendingItems) {
        this.pendingItems = pendingItems;
    }
}
