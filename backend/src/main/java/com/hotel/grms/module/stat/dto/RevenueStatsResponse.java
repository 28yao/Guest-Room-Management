package com.hotel.grms.module.stat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 区间房费营收响应（与 payment 流水净额一致）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class RevenueStatsResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalRevenue;
    private BigDecimal cashTotal;
    private BigDecimal wechatTotal;
    private BigDecimal alipayTotal;
    private List<RevenueDailyItem> dailyItems;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
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

    public List<RevenueDailyItem> getDailyItems() {
        return dailyItems;
    }

    public void setDailyItems(List<RevenueDailyItem> dailyItems) {
        this.dailyItems = dailyItems;
    }
}
