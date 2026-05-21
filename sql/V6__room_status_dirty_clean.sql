-- 前台置脏房、保洁置空净权限（已有库增量）
-- 依赖: V2__seed_data.sql

SET NAMES utf8mb4;

INSERT INTO sys_permission (code, name, description) VALUES
('room:status:dirty', '设为脏房', '前台将客房置为脏房'),
('room:status:clean', '设为空净', '保洁将脏房置为空净')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
JOIN sys_permission p ON p.code = 'room:status:dirty'
WHERE r.code IN ('ROLE_FRONT_DESK', 'ROLE_MANAGER')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
JOIN sys_permission p ON p.code = 'room:status:clean'
WHERE r.code IN ('ROLE_HOUSEKEEPING', 'ROLE_MANAGER')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
