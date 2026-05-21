-- 恢复置脏/置净权限（管理员角色若被改过会导致房态切换 403）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('room:status:dirty', 'room:status:clean')
WHERE r.code IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FRONT_DESK', 'ROLE_HOUSEKEEPING')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );
