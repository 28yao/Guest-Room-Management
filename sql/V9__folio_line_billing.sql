-- V9: 账单明细补齐 quantity、unit_price（与 V1、BillingService 写入一致）
-- 若列已存在会报错，可忽略该条 ALTER

USE grms;

ALTER TABLE folio_line
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量' AFTER description;

ALTER TABLE folio_line
    ADD COLUMN unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '单价' AFTER quantity;

UPDATE folio_line
SET unit_price = amount
WHERE unit_price = 0 AND amount IS NOT NULL;
