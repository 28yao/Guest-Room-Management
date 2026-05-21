-- 房态图、在住管理访问权限（保洁角色不授予）
-- 1) 写入 sys_permission，供「角色权限配置」「敏感权限直授」勾选
-- 2) 授予 ROLE_ADMIN / ROLE_MANAGER / ROLE_FRONT_DESK（不含 ROLE_HOUSEKEEPING）
-- 旧库若 V2 执行时尚无此二权限点，须单独执行本脚本
-- 依赖：V1、V2 已执行

SET NAMES utf8mb4;

-- Windows PowerShell 执行请加：-Encoding UTF8 与 mysql --default-character-set=utf8mb4（见 README）

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
