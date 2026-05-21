INSERT INTO sys_role (id, code, name) VALUES (1, 'ROLE_ADMIN', '系统管理员');
INSERT INTO sys_role (id, code, name) VALUES (2, 'ROLE_FRONT_DESK', '前台');
INSERT INTO sys_role (id, code, name) VALUES (3, 'ROLE_HOUSEKEEPING', '保洁');

INSERT INTO sys_permission (id, code, name) VALUES (1, 'system:user:manage', '用户管理');
INSERT INTO sys_permission (id, code, name) VALUES (2, 'system:role:manage', '角色权限配置');
INSERT INTO sys_permission (id, code, name) VALUES (3, 'reservation:manage', '预订管理');
INSERT INTO sys_permission (id, code, name) VALUES (4, 'room:type:manage', '房型维护');
INSERT INTO sys_permission (id, code, name) VALUES (5, 'room:manage', '客房维护');
INSERT INTO sys_permission (id, code, name) VALUES (6, 'room:status:maintenance', '客房维修');
INSERT INTO sys_permission (id, code, name) VALUES (7, 'room:status:force', '强制改房态');
INSERT INTO sys_permission (id, code, name) VALUES (8, 'room:status:dirty', '设为脏房');
INSERT INTO sys_permission (id, code, name) VALUES (9, 'room:status:clean', '设为空净');
INSERT INTO sys_permission (id, code, name) VALUES (10, 'stay:checkin', '办理入住');
INSERT INTO sys_permission (id, code, name) VALUES (11, 'stay:change_room', '换房');
INSERT INTO sys_permission (id, code, name) VALUES (12, 'shift:open', '开班');
INSERT INTO sys_permission (id, code, name) VALUES (13, 'shift:close', '结班');
INSERT INTO sys_permission (id, code, name) VALUES (14, 'billing:price:adjust', '改价');
INSERT INTO sys_permission (id, code, name) VALUES (15, 'billing:checkout', '退房结账');
INSERT INTO sys_permission (id, code, name) VALUES (16, 'hk:view', '查看保洁任务');
INSERT INTO sys_permission (id, code, name) VALUES (17, 'hk:complete', '完成保洁');
INSERT INTO sys_permission (id, code, name) VALUES (18, 'room:board:view', '查看房态图');
INSERT INTO sys_permission (id, code, name) VALUES (19, 'stay:in_house:view', '在住管理');
INSERT INTO sys_permission (id, code, name) VALUES (20, 'stat:view', '经营统计');
INSERT INTO sys_permission (id, code, name) VALUES (21, 'audit:view', '审计查询');

INSERT INTO sys_user (id, username, password, status) VALUES
(1, 'admin', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);
INSERT INTO sys_user (id, username, password, status) VALUES
(2, 'front', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);

INSERT INTO sys_user_role VALUES (1, 1);
INSERT INTO sys_user_role VALUES (2, 2);

INSERT INTO sys_role_permission SELECT 1, id FROM sys_permission;

INSERT INTO sys_role_permission VALUES (2, 3);
INSERT INTO sys_role_permission VALUES (2, 8);
INSERT INTO sys_role_permission VALUES (2, 10);
INSERT INTO sys_role_permission VALUES (2, 11);
INSERT INTO sys_role_permission VALUES (2, 12);
INSERT INTO sys_role_permission VALUES (2, 13);
INSERT INTO sys_role_permission VALUES (2, 15);
INSERT INTO sys_role_permission VALUES (2, 18);
INSERT INTO sys_role_permission VALUES (2, 19);

INSERT INTO sys_role_permission VALUES (3, 9);
INSERT INTO sys_role_permission VALUES (3, 16);
INSERT INTO sys_role_permission VALUES (3, 17);

INSERT INTO sys_user (id, username, password, status) VALUES
(3, 'hk01', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);
INSERT INTO sys_user_role VALUES (3, 3);
