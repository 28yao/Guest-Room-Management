package com.hotel.grms.module.stay.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 在住提前退房（退订退款）请求。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class VoidCheckoutRequest {

    /** 房费计至日期（含该晚），默认当天 */
    private LocalDate chargeThroughDate;

    /** 退款金额，null 时按已付减应付自动计算 */
    @DecimalMin(value = "0", message = "退款金额不能为负")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款方式不能为空")
    private String refundMethod;

    private String remark;

    public LocalDate getChargeThroughDate() {
        return chargeThroughDate;
    }

    public void setChargeThroughDate(LocalDate chargeThroughDate) {
        this.chargeThroughDate = chargeThroughDate;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
