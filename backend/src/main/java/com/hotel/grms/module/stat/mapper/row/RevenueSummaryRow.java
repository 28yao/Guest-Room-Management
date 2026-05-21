package com.hotel.grms.module.stat.mapper.row;

import java.math.BigDecimal;

/**
 * 营收区间汇总查询结果行。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class RevenueSummaryRow {

    private BigDecimal totalAmount;
    private BigDecimal cashTotal;
    private BigDecimal wechatTotal;
    private BigDecimal alipayTotal;

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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
}
