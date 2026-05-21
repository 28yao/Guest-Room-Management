package com.hotel.grms.module.shift.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.shift.dto.ShiftCloseRequest;
import com.hotel.grms.module.shift.dto.ShiftHandoverPreviewResponse;
import com.hotel.grms.module.shift.dto.ShiftHandoverResponse;
import com.hotel.grms.module.shift.dto.ShiftSessionResponse;
import com.hotel.grms.module.shift.service.ShiftHandoverService;
import com.hotel.grms.module.shift.service.ShiftSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交班 REST 接口：开班、结班预览与结班。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftSessionService shiftSessionService;
    private final ShiftHandoverService shiftHandoverService;

    public ShiftController(ShiftSessionService shiftSessionService, ShiftHandoverService shiftHandoverService) {
        this.shiftSessionService = shiftSessionService;
        this.shiftHandoverService = shiftHandoverService;
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

    /**
     * 结班预览。
     *
     * @param id 开班 ID
     * @return 收款汇总与待办
     */
    @GetMapping("/{id}/handover-preview")
    @PreAuthorize("hasAuthority('shift:close')")
    public R<ShiftHandoverPreviewResponse> handoverPreview(@PathVariable Long id) {
        return R.ok(shiftHandoverService.buildPreview(id));
    }

    /**
     * 结班。
     *
     * @param id      开班 ID
     * @param request 可选强制结班
     * @return 结班单
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('shift:close')")
    public R<ShiftHandoverResponse> close(@PathVariable Long id, @RequestBody(required = false) ShiftCloseRequest request) {
        return R.ok(shiftHandoverService.closeShift(id, request));
    }

    /**
     * 结班单详情。
     *
     * @param id 结班单 ID
     * @return 详情
     */
    @GetMapping("/handover/{id}")
    public R<ShiftHandoverResponse> handover(@PathVariable Long id) {
        return R.ok(shiftHandoverService.getHandover(id));
    }
}
