-- 演示用房型与客房（可选，便于房态图验收）
USE grms;

INSERT INTO room_type (name, description, rack_rate, bed_type, max_guests, status) VALUES
('标准大床', '28㎡ 大床', 299.00, '大床', 2, 1),
('标准双床', '28㎡ 双床', 319.00, '双床', 2, 1);

INSERT INTO room (room_no, room_type_id, floor_no, status, version)
SELECT CONCAT('2', LPAD(n.n, 2, '0')), (SELECT id FROM room_type WHERE name = '标准大床' LIMIT 1), 2, 'VACANT_CLEAN', 0
FROM (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) n;

INSERT INTO room (room_no, room_type_id, floor_no, status, version)
SELECT CONCAT('3', LPAD(n.n, 2, '0')), (SELECT id FROM room_type WHERE name = '标准双床' LIMIT 1), 3, 'VACANT_CLEAN', 0
FROM (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
) n;
