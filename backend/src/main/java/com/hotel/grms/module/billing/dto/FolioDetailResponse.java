package com.hotel.grms.module.billing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账单详情响应（含明细与待结余额）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class FolioDetailResponse {

    private Long id;
    private Long stayOrderId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private String status;
    private List<FolioLineResponse> lines;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStayOrderId() {
        return stayOrderId;
    }

    public void setStayOrderId(Long stayOrderId) {
        this.stayOrderId = stayOrderId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FolioLineResponse> getLines() {
        return lines;
    }

    public void setLines(List<FolioLineResponse> lines) {
        this.lines = lines;
    }
}
