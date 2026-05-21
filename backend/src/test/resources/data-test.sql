INSERT INTO sys_role (id, code, name) VALUES (1, 'ROLE_ADMIN', '系统管理员');
INSERT INTO sys_role (id, code, name) VALUES (2, 'ROLE_FRONT_DESK', '前台');

INSERT INTO sys_permission (id, code, name) VALUES (1, 'system:user:manage', '用户管理');
INSERT INTO sys_permission (id, code, name) VALUES (2, 'system:role:manage', '角色权限配置');
INSERT INTO sys_permission (id, code, name) VALUES (3, 'reservation:manage', '预订管理');

INSERT INTO sys_user (id, username, password, status) VALUES
(1, 'admin', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);
INSERT INTO sys_user (id, username, password, status) VALUES
(2, 'front', '$2a$10$6am6wYerYdF3L6tzBP7LNeVP13IkCRu7OMClrkSfpOaO/KgQKw4/C', 1);

INSERT INTO sys_user_role VALUES (1, 1);
INSERT INTO sys_user_role VALUES (2, 2);

INSERT INTO sys_role_permission SELECT 1, id FROM sys_permission;

INSERT INTO sys_role_permission VALUES (2, 3);
