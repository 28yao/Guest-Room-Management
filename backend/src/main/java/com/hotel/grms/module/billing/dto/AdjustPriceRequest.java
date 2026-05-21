package com.hotel.grms.module.billing.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 改价请求：更新在住单协议日价并重算账单。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class AdjustPriceRequest {

    @NotNull(message = "协议日价不能为空")
    @DecimalMin(value = "0.01", message = "协议日价须大于 0")
    private BigDecimal agreedDailyRate;

    public BigDecimal getAgreedDailyRate() {
        return agreedDailyRate;
    }

    public void setAgreedDailyRate(BigDecimal agreedDailyRate) {
        this.agreedDailyRate = agreedDailyRate;
    }
}
