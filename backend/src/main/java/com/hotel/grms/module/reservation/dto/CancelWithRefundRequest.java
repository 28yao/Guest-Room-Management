package com.hotel.grms.module.reservation.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 预订退订（退款）请求。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class CancelWithRefundRequest {

    @DecimalMin(value = "0", message = "退款金额不能为负")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款方式不能为空")
    private String refundMethod;

    private String remark;

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
