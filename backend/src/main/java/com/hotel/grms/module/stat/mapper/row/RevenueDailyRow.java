package com.hotel.grms.module.stat.mapper.row;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 按日营收查询结果行。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class RevenueDailyRow {

    private LocalDate statDate;
    private BigDecimal totalAmount;

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
