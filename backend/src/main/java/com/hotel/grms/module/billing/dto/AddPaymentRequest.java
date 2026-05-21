package com.hotel.grms.module.billing.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 账单收款请求。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class AddPaymentRequest {

    @NotBlank(message = "支付方式不能为空")
    private String method;

    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额须大于 0")
    private BigDecimal amount;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
