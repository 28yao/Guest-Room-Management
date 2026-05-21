-- V18: operation_log align for MOD-AUDIT (legacy: action column -> operation_type)
-- Ignore error if column already renamed.

USE grms;

ALTER TABLE operation_log
    CHANGE COLUMN action operation_type VARCHAR(64) NOT NULL COMMENT 'operation type code';
