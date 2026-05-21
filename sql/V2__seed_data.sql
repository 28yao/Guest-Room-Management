-- GRMS MVP 种子数据
-- 默认管理员: username=admin  password=admin123 (BCrypt)
-- 依赖: V1__init_schema.sql

SET NAMES utf8mb4;

-- 系统配置 (plan §11)
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('billing.mode', 'PER_NIGHT', '计费模式：按晚'),
('billing.departure_day_charge', 'false', '离店当天是否计费'),
('shift.require_open', 'true', '收款前须开班'),
('shift.block_close_if_pending', 'true', '有待办阻断结班'),
('hotel.name', '示例酒店', '酒店名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 权限点 (plan §4.2)
INSERT INTO sys_permission (code, name, description) VALUES
('system:user:manage', '用户管理', '用户增删改停用'),
('system:role:manage', '角色权限配置', '角色权限配置'),
('system:permission:grant', '敏感权限直授', '向用户直授敏感权'),
('room:type:manage', '房型维护', '房型维护'),
('room:manage', '客房维护', '客房维护'),
('room:status:maintenance', '客房维修', '设维修/结束维修'),
('room:status:dirty', '设为脏房', '前台将客房置为脏房'),
('room:status:clean', '设为空净', '保洁将脏房置为空净'),
('room:status:force', '强制改房态', '强制改房态'),
('reservation:manage', '预订管理', '预订 CRUD、释放'),
('stay:checkin', '办理入住', '入住'),
('stay:change_room', '换房', '换房'),
('billing:price:adjust', '改价', '修改房价/折扣'),
('billing:checkout', '退房结账', '退房结账'),
('hk:view', '查看保洁任务', '待扫列表'),
('hk:complete', '完成保洁', '完成保洁'),
('shift:open', '开班', '开班'),
('shift:close', '结班', '结班'),
('shift:force_close', '强制结班', '有待办仍结班'),
('stat:view', '经营统计', '出租率/营收'),
('audit:view', '审计查询', '审计查询'),
('room:board:view', '查看房态图', '房态图、客房日程、楼层筛选'),
('stay:in_house:view', '在住管理', '在住列表查询');

-- 角色
INSERT INTO sys_role (code, name, description) VALUES
('ROLE_ADMIN', '系统管理员', '系统配置与用户权限'),
('ROLE_MANAGER', '店长', '运营监督与统计'),
('ROLE_FRONT_DESK', '前台', '预订入住退房交班'),
('ROLE_HOUSEKEEPING', '保洁', '保洁任务');

-- 管理员账号 admin / admin123
INSERT INTO sys_user (username, password, status) VALUES
('admin', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN';

-- 保洁演示账号 hk01 / admin123（无房态图、在住管理权限）
INSERT INTO sys_user (username, password, status) VALUES
('hk01', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'hk01' AND r.code = 'ROLE_HOUSEKEEPING';

-- 角色-权限：管理员拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'ROLE_ADMIN';

-- 店长权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.code IN (
    'room:manage', 'room:status:maintenance', 'room:status:dirty', 'room:status:clean', 'room:status:force',
    'stat:view', 'audit:view', 'shift:force_close', 'reservation:manage',
    'stay:checkin', 'stay:change_room', 'billing:checkout', 'hk:view',
    'room:board:view', 'stay:in_house:view'
) WHERE r.code = 'ROLE_MANAGER';

-- 前台权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.code IN (
    'reservation:manage', 'stay:checkin', 'stay:change_room',
    'billing:checkout', 'shift:open', 'shift:close',
    'room:status:maintenance', 'room:status:dirty', 'room:status:clean', 'hk:view',
    'room:board:view', 'stay:in_house:view'
) WHERE r.code = 'ROLE_FRONT_DESK';

-- 保洁权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.code IN (
    'hk:view', 'hk:complete', 'room:status:clean'
) WHERE r.code = 'ROLE_HOUSEKEEPING';
