package com.hotel.grms.module.hk.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.hk.dto.HkTaskResponse;
import com.hotel.grms.module.hk.service.HousekeepingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 保洁任务 REST 接口：待扫列表与完成打扫。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/hk")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    public HousekeepingController(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    /**
     * 查询待打扫任务列表。
     *
     * @param floorNo 可选楼层筛选
     * @param status  任务状态，默认 PENDING
     * @return 任务列表
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('hk:view')")
    public R<List<HkTaskResponse>> listTasks(
            @RequestParam(required = false) Integer floorNo,
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        return R.ok(housekeepingService.listTasks(floorNo, status));
    }

    /**
     * 完成保洁：任务关闭并将客房置为空净。
     *
     * @param id 任务 ID
     * @return 更新后任务
     */
    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasAuthority('hk:complete')")
    public R<HkTaskResponse> completeTask(@PathVariable Long id) {
        return R.ok(housekeepingService.completeTask(id));
    }
}
