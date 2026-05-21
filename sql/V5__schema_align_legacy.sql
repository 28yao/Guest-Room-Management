-- 为早期 grms 库补齐与 V1 不一致的列（可重复执行前请自行确认列是否已存在）
USE grms;

-- room_type.status（实体 RoomType.status）
ALTER TABLE room_type
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用' AFTER max_guests;

-- room.created_at（实体 Room.createdAt）
ALTER TABLE room
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER version;

-- stay_order 时间戳（房态图/后续模块用）
ALTER TABLE stay_order
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER check_out_at;

ALTER TABLE stay_order
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
