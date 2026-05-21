-- V8: 在住单主表补齐客人姓名/电话（与早期 grms 库、入住写入一致）
-- 若列已存在会报错，可忽略该条 ALTER

USE grms;

ALTER TABLE stay_order
    ADD COLUMN guest_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '主客人姓名' AFTER room_type_id;

ALTER TABLE stay_order
    ADD COLUMN guest_phone VARCHAR(20) NOT NULL DEFAULT '' COMMENT '主客人电话' AFTER guest_name;
