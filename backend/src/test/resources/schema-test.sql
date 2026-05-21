CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_user_permission (
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, permission_id)
);

CREATE TABLE IF NOT EXISTS room_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    rack_rate DECIMAL(10, 2) NOT NULL,
    bed_type VARCHAR(32),
    window_type VARCHAR(32),
    non_smoking TINYINT NOT NULL DEFAULT 0,
    max_guests INT NOT NULL DEFAULT 2,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_no VARCHAR(16) NOT NULL UNIQUE,
    room_type_id BIGINT NOT NULL,
    floor_no INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    clean_status VARCHAR(32) NOT NULL DEFAULT 'CLEAN',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room_maintenance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    reason VARCHAR(512) NOT NULL,
    expected_recovery_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    operator_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    res_no VARCHAR(32) NOT NULL UNIQUE,
    guest_name VARCHAR(64) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    room_type_id BIGINT,
    room_id BIGINT,
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,
    arrival_at TIMESTAMP NOT NULL,
    departure_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stay_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stay_no VARCHAR(32) NOT NULL UNIQUE,
    reservation_id BIGINT,
    room_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    guest_name VARCHAR(64) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,
    agreed_daily_rate DECIMAL(10, 2),
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512),
    check_in_at TIMESTAMP NOT NULL,
    check_out_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stay_guest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stay_order_id BIGINT NOT NULL,
    guest_name VARCHAR(64) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    id_card VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS folio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stay_order_id BIGINT NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS folio_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    folio_id BIGINT NOT NULL,
    line_type VARCHAR(32) NOT NULL DEFAULT 'ROOM',
    description VARCHAR(256),
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    active TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shift_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    status VARCHAR(16) NOT NULL
);
