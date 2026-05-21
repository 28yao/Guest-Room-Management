-- 前台房态图空净/脏房双向切换：补充「设为空净」权限（与保洁共用，不新增权限点）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = 'room:status:clean'
WHERE r.code = 'ROLE_FRONT_DESK'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );
