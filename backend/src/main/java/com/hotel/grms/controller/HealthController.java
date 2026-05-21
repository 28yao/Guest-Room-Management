package com.hotel.grms.controller;

import com.hotel.grms.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查接口，用于部署与冒烟验证。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * 返回应用健康状态。
     *
     * @return 健康信息
     */
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<String, Object>(4);
        body.put("status", "UP");
        body.put("application", "grms-backend");
        return R.ok(body);
    }
}
