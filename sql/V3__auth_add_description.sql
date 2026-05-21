-- 为早期建库补齐 description 列（与 V1__init_schema.sql、实体字段一致）
USE grms;

ALTER TABLE sys_role
    ADD COLUMN description VARCHAR(256) DEFAULT NULL COMMENT '角色说明' AFTER name;

ALTER TABLE sys_permission
    ADD COLUMN description VARCHAR(256) DEFAULT NULL COMMENT '权限说明' AFTER name;
