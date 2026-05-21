-- GRMS MVP 初始化表结构（仅 CREATE，禁止 DROP）
-- 需求: specs/plan.md §5.1

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password        VARCHAR(128) NOT NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    description     VARCHAR(256) DEFAULT NULL,
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(256) DEFAULT NULL,
    UNIQUE KEY uk_sys_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_sys_role_permission_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS sys_user_permission (
    user_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    KEY idx_sys_user_permission_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户敏感权限直授';

CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(128) NOT NULL,
    config_value    VARCHAR(512) NOT NULL,
    remark          VARCHAR(256) DEFAULT NULL,
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS room_type (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)    NOT NULL,
    description     VARCHAR(256)   DEFAULT NULL,
    rack_rate       DECIMAL(10, 2) NOT NULL,
    bed_type        VARCHAR(32)    DEFAULT NULL,
    window_type     VARCHAR(32)    DEFAULT NULL,
    non_smoking     TINYINT        NOT NULL DEFAULT 0,
    max_guests      INT            NOT NULL DEFAULT 2,
    status          TINYINT        NOT NULL DEFAULT 1,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型';

CREATE TABLE IF NOT EXISTS room (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_no         VARCHAR(16)  NOT NULL,
    room_type_id    BIGINT       NOT NULL,
    floor_no        INT          NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    version         INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_no (room_no),
    KEY idx_room_status_floor (status, floor_no),
    KEY idx_room_type (room_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客房';

CREATE TABLE IF NOT EXISTS room_maintenance_log (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id                 BIGINT       NOT NULL,
    reason                  VARCHAR(512) NOT NULL,
    expected_recovery_at    DATETIME     NOT NULL,
    started_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at                DATETIME     DEFAULT NULL,
    operator_id             BIGINT       NOT NULL,
    KEY idx_room_maint_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客房维修记录';

CREATE TABLE IF NOT EXISTS reservation (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    res_no          VARCHAR(32)  NOT NULL,
    guest_name      VARCHAR(64)  NOT NULL,
    guest_phone     VARCHAR(20)  NOT NULL,
    room_type_id    BIGINT       DEFAULT NULL,
    room_id         BIGINT       DEFAULT NULL,
    arrival_date    DATE         NOT NULL,
    departure_date  DATE         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    remark          VARCHAR(512) DEFAULT NULL,
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (res_no),
    KEY idx_reservation_arrival_status (arrival_date, status),
    KEY idx_reservation_phone (guest_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预订';

CREATE TABLE IF NOT EXISTS stay_order (
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stay_no             VARCHAR(32)    NOT NULL,
    reservation_id      BIGINT         DEFAULT NULL,
    room_id             BIGINT         NOT NULL,
    room_type_id        BIGINT         NOT NULL,
    arrival_date        DATE           NOT NULL,
    departure_date      DATE           NOT NULL,
    agreed_daily_rate   DECIMAL(10, 2) DEFAULT NULL,
    status              VARCHAR(32)    NOT NULL,
    remark              VARCHAR(512)   DEFAULT NULL,
    check_in_at         DATETIME       NOT NULL,
    check_out_at        DATETIME       DEFAULT NULL,
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stay_no (stay_no),
    KEY idx_stay_status_room (status, room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在住订单';

CREATE TABLE IF NOT EXISTS stay_guest (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stay_order_id   BIGINT       NOT NULL,
    guest_name      VARCHAR(64)  NOT NULL,
    guest_phone     VARCHAR(20)  NOT NULL,
    id_card         VARCHAR(32)  DEFAULT NULL,
    KEY idx_stay_guest_order (stay_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在住客人';

CREATE TABLE IF NOT EXISTS folio (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stay_order_id   BIGINT         NOT NULL,
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0,
    paid_amount     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(32)    NOT NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_folio_stay (stay_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单';

CREATE TABLE IF NOT EXISTS folio_line (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    folio_id        BIGINT         NOT NULL,
    line_type       VARCHAR(32)    NOT NULL DEFAULT 'ROOM',
    description     VARCHAR(256)   DEFAULT NULL,
    quantity        INT            NOT NULL DEFAULT 1,
    unit_price      DECIMAL(10, 2) NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    active          TINYINT        NOT NULL DEFAULT 1,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_folio_line_folio (folio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细';

CREATE TABLE IF NOT EXISTS payment (
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    folio_id            BIGINT         NOT NULL,
    shift_session_id    BIGINT         DEFAULT NULL,
    method              VARCHAR(32)    NOT NULL,
    amount              DECIMAL(10, 2) NOT NULL,
    paid_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id         BIGINT         NOT NULL,
    KEY idx_payment_shift (shift_session_id, paid_at),
    KEY idx_payment_folio (folio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水';

CREATE TABLE IF NOT EXISTS hk_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id         BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME     DEFAULT NULL,
    KEY idx_hk_task_status_room (status, room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保洁任务';

CREATE TABLE IF NOT EXISTS shift_session (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT       NOT NULL,
    opened_at       DATETIME     NOT NULL,
    closed_at       DATETIME     DEFAULT NULL,
    status          VARCHAR(16)  NOT NULL,
    KEY idx_shift_operator_status (operator_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开班记录';

CREATE TABLE IF NOT EXISTS shift_handover (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_session_id    BIGINT       NOT NULL,
    cash_total          DECIMAL(10, 2) NOT NULL DEFAULT 0,
    wechat_total        DECIMAL(10, 2) NOT NULL DEFAULT 0,
    alipay_total        DECIMAL(10, 2) NOT NULL DEFAULT 0,
    pending_snapshot    JSON         DEFAULT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shift_handover_session (shift_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结班快照';

CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    biz_type        VARCHAR(32)  NOT NULL,
    biz_id          BIGINT       NOT NULL,
    operation_type  VARCHAR(64)  NOT NULL,
    operator_id     BIGINT       NOT NULL,
    operator_name   VARCHAR(64)  NOT NULL,
    before_value    TEXT         DEFAULT NULL,
    after_value     TEXT         DEFAULT NULL,
    summary         VARCHAR(512) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_log_biz (biz_type, biz_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计';
