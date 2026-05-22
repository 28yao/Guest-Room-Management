package com.hotel.grms.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 集成测试业务数据清理，保证 {@code @SpringBootTest} 用例可重复执行。
 * H2 在 {@code spring.sql.init} 下仅初始化 schema/种子用户，业务表由各用例自行清理。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public final class GrmsTestDataCleaner {

    private GrmsTestDataCleaner() {
    }

    /**
     * 按外键依赖顺序删除可变的业务数据（保留 sys_* 种子）。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public static void cleanTransactionalData(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM operation_log");
        jdbcTemplate.update("DELETE FROM shift_handover");
        jdbcTemplate.update("DELETE FROM hk_task");
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM folio_line");
        jdbcTemplate.update("DELETE FROM folio");
        jdbcTemplate.update("DELETE FROM stay_guest");
        jdbcTemplate.update("DELETE FROM stay_order");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM room_maintenance_log");
        jdbcTemplate.update("DELETE FROM shift_session");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM room_type");
    }
}
