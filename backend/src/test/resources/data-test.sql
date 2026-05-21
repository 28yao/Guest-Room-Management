INSERT INTO sys_role (id, code, name) VALUES (1, 'ROLE_ADMIN', '系统管理员');
INSERT INTO sys_role (id, code, name) VALUES (2, 'ROLE_FRONT_DESK', '前台');

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
