package com.hotel.grms.module.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.billing.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付/退款流水 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
