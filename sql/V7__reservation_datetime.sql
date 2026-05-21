-- V7: 预订精确时刻（入住/离店 datetime），默认回填 18:00 / 12:00
-- 执行前请确认环境为开发/测试；生产须备份后执行

USE grms;

ALTER TABLE reservation
    ADD COLUMN arrival_at DATETIME NULL COMMENT '入住时刻' AFTER departure_date,
    ADD COLUMN departure_at DATETIME NULL COMMENT '离店时刻' AFTER arrival_at;

UPDATE reservation
SET arrival_at   = CONCAT(arrival_date, ' 18:00:00'),
    departure_at = CONCAT(departure_date, ' 12:00:00')
WHERE arrival_at IS NULL;

ALTER TABLE reservation
    MODIFY COLUMN arrival_at DATETIME NOT NULL COMMENT '入住时刻',
    MODIFY COLUMN departure_at DATETIME NOT NULL COMMENT '离店时刻';
