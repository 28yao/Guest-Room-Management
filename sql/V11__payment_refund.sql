-- 预订退订退款：支付流水 folio_id 允许为空（仅记当班退款）
ALTER TABLE payment MODIFY folio_id BIGINT NULL COMMENT '账单ID，预订退款可为空';
