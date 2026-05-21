package com.hotel.grms.module.billing.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.billing.dto.AddPaymentRequest;
import com.hotel.grms.module.billing.dto.AdjustPriceRequest;
import com.hotel.grms.module.billing.dto.FolioDetailResponse;
import com.hotel.grms.module.billing.service.BillingService;
import com.hotel.grms.module.shift.service.ShiftSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账单与收款 REST 接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/folios")
public class FolioController {

    private final BillingService billingService;
    private final ShiftSessionService shiftSessionService;

    public FolioController(BillingService billingService, ShiftSessionService shiftSessionService) {
        this.billingService = billingService;
        this.shiftSessionService = shiftSessionService;
    }

    /**
     * 按在住单查询账单。
     *
     * @param stayId 在住单 ID
     * @return 账单详情
     */
    @GetMapping("/by-stay/{stayId}")
    public R<FolioDetailResponse> getByStay(@PathVariable Long stayId) {
        return R.ok(billingService.getFolioDetailByStay(stayId));
    }

    /**
     * 整段重算账单（换房后亦可手动触发）。
     *
     * @param id 账单 ID
     * @return 账单详情
     */
    @PostMapping("/{id}/recalculate")
    public R<FolioDetailResponse> recalculate(@PathVariable Long id) {
        FolioDetailResponse current = billingService.getFolioDetail(id);
        billingService.recalculateFullStay(current.getStayOrderId());
        return R.ok(billingService.getFolioDetail(id));
    }

    /**
     * 改价并重算。
     *
     * @param id      账单 ID
     * @param request 请求体
     * @return 账单详情
     */
    @PostMapping("/{id}/adjust-price")
    @PreAuthorize("hasAuthority('billing:price:adjust')")
    public R<FolioDetailResponse> adjustPrice(@PathVariable Long id, @Validated @RequestBody AdjustPriceRequest request) {
        return R.ok(billingService.adjustAgreedDailyRate(id, request.getAgreedDailyRate()));
    }

    /**
     * 分笔收款。
     *
     * @param id      账单 ID
     * @param request 请求体
     * @return 账单详情
     */
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('billing:checkout')")
    public R<FolioDetailResponse> addPayment(@PathVariable Long id, @Validated @RequestBody AddPaymentRequest request) {
        Long shiftId = shiftSessionService.requireOpenSessionId();
        return R.ok(billingService.addPayment(id, request.getAmount(), request.getMethod(), shiftId));
    }
}
