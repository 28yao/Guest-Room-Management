-- 保洁态与占用态分离：clean_status=CLEAN|DIRTY，status 仅存占用态 VACANT|RESERVED|OCCUPIED|OUT_OF_ORDER
ALTER TABLE room ADD COLUMN clean_status VARCHAR(32) NOT NULL DEFAULT 'CLEAN' COMMENT '保洁态 CLEAN/DIRTY' AFTER status;

UPDATE room SET clean_status = 'DIRTY' WHERE status = 'DIRTY';
UPDATE room SET status = 'VACANT' WHERE status IN ('DIRTY', 'VACANT_CLEAN');
