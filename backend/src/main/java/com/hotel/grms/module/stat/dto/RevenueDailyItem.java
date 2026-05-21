package com.hotel.grms.module.stat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 按日房费营收项。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class RevenueDailyItem {

    private LocalDate date;
    private BigDecimal amount;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
