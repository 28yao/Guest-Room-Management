-- V10: 账单主表补齐 created_at、updated_at（换房/重算时查询 folio 需要）
-- 若列已存在会报错，可忽略该条 ALTER

USE grms;

ALTER TABLE folio
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER status;

ALTER TABLE folio
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;
