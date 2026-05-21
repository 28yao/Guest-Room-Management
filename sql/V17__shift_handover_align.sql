-- V17: shift_handover align for MOD-SHIFT (legacy: snapshot_json only, no cash_total columns)
-- Ignore Duplicate column if a step was already applied.

USE grms;

ALTER TABLE shift_handover
    ADD COLUMN cash_total DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER shift_session_id;

ALTER TABLE shift_handover
    ADD COLUMN wechat_total DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER cash_total;

ALTER TABLE shift_handover
    ADD COLUMN alipay_total DECIMAL(10, 2) NOT NULL DEFAULT 0 AFTER wechat_total;

ALTER TABLE shift_handover
    CHANGE COLUMN snapshot_json pending_snapshot TEXT NULL;
