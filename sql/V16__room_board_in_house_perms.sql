-- 房态图、在住管理访问权限（保洁角色不授予）
-- 依赖：V2 种子已执行

SET NAMES utf8mb4;

INSERT INTO sys_permission (code, name, description) VALUES
('room:board:view', '查看房态图', '房态图、客房日程、楼层筛选'),
('stay:in_house:view', '在住管理', '在住列表查询')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
JOIN sys_permission p ON p.code IN ('room:board:view', 'stay:in_house:view')
WHERE r.code IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FRONT_DESK')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
