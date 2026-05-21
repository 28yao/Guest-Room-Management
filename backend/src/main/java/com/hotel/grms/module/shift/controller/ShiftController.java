package com.hotel.grms.module.shift.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.shift.dto.ShiftSessionResponse;
import com.hotel.grms.module.shift.service.ShiftSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交班开班 REST 接口（MVP：开班与当前班查询）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftSessionService shiftSessionService;

    public ShiftController(ShiftSessionService shiftSessionService) {
        this.shiftSessionService = shiftSessionService;
    }

    /**
     * 开班。
     *
     * @return 当前开班会话
     */
    @PostMapping("/open")
    @PreAuthorize("hasAuthority('shift:open')")
    public R<ShiftSessionResponse> open() {
        return R.ok(shiftSessionService.openCurrent());
    }

    /**
     * 查询当前操作员开班状态。
     *
     * @return 会话或 null
     */
    @GetMapping("/current")
    public R<ShiftSessionResponse> current() {
        return R.ok(shiftSessionService.getCurrent());
    }
}
