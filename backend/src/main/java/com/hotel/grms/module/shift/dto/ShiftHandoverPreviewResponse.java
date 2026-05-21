package com.hotel.grms.module.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结班预览响应：本班收款汇总与待办事项。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class ShiftHandoverPreviewResponse {

    private Long shiftSessionId;
    private LocalDateTime openedAt;
    private BigDecimal cashTotal;
    private BigDecimal wechatTotal;
    private BigDecimal alipayTotal;
    private int pendingCount;
    private boolean blockCloseOnPending;
    private List<HandoverPendingItem> pendingItems;

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

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public boolean isBlockCloseOnPending() {
        return blockCloseOnPending;
    }

    public void setBlockCloseOnPending(boolean blockCloseOnPending) {
        this.blockCloseOnPending = blockCloseOnPending;
    }

    public List<HandoverPendingItem> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<HandoverPendingItem> pendingItems) {
        this.pendingItems = pendingItems;
    }
}
