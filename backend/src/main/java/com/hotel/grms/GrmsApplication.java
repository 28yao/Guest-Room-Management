package com.hotel.grms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 酒店客房管理系统启动入口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootApplication
public class GrmsApplication {

    /**
     * 应用主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GrmsApplication.class, args);
    }
}
