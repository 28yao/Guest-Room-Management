package com.hotel.grms.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成种子数据用 BCrypt 哈希（开发辅助）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
class BcryptHashGeneratorTest {

    /**
     * 输出 admin123 的 BCrypt，用于更新 V2__seed_data.sql。
     */
    @Test
    void printAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("admin123 BCrypt: " + hash);
    }
}
