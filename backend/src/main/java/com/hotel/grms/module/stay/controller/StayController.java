package com.hotel.grms.module.stay.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.stay.dto.ChangeRoomRequest;
import com.hotel.grms.module.stay.dto.CheckInFromReservationRequest;
import com.hotel.grms.module.stay.dto.StayRemarkRequest;
import com.hotel.grms.module.stay.dto.StayResponse;
import com.hotel.grms.module.stay.dto.VoidCheckoutRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import com.hotel.grms.module.billing.service.CheckoutService;
import com.hotel.grms.module.stay.service.StayService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 入住与在住 REST 接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/stays")
public class StayController {

    private final StayService stayService;
    private final CheckoutService checkoutService;

    public StayController(StayService stayService, CheckoutService checkoutService) {
        this.stayService = stayService;
        this.checkoutService = checkoutService;
    }

    /**
     * Walk-in 入住。
     *
     * @param request 请求体
     * @return 在住单
     */
    @PostMapping("/walk-in")
    @PreAuthorize("hasAuthority('stay:checkin')")
    public R<StayResponse> walkIn(@Validated @RequestBody WalkInCheckInRequest request) {
        return R.ok(stayService.checkInWalkIn(request));
    }

    /**
     * 预订入住。
     *
     * @param request 请求体
     * @return 在住单
     */
    @PostMapping("/check-in-from-reservation")
    @PreAuthorize("hasAuthority('stay:checkin')")
    public R<StayResponse> checkInFromReservation(@Validated @RequestBody CheckInFromReservationRequest request) {
        return R.ok(stayService.checkInFromReservation(request));
    }

    /**
     * 在住列表。
     *
     * @return 列表
     */
    /**
     * 在住列表，支持客人姓名模糊查询。
     *
     * @param guestName 客人姓名（可选）
     * @return 列表
     */
    @GetMapping("/in-house")
    @PreAuthorize("hasAuthority('stay:in_house:view')")
    public R<List<StayResponse>> inHouse(@RequestParam(required = false) String guestName) {
        return R.ok(stayService.listInHouse(guestName));
    }

    /**
     * 在住详情。
     *
     * @param id 在住单 ID
     * @return 详情
     */
    @GetMapping("/{id}")
    public R<StayResponse> get(@PathVariable Long id) {
        return R.ok(stayService.getById(id));
    }

    /**
     * 换房。
     *
     * @param id      在住单 ID
     * @param request 请求体
     * @return 更新后详情
     */
    @PostMapping("/{id}/change-room")
    @PreAuthorize("hasAuthority('stay:change_room')")
    public R<StayResponse> changeRoom(@PathVariable Long id, @Validated @RequestBody ChangeRoomRequest request) {
        return R.ok(stayService.changeRoom(id, request));
    }

    /**
     * 更新备注。
     *
     * @param id      在住单 ID
     * @param request 请求体
     * @return 更新后详情
     */
    @PutMapping("/{id}/remark")
    public R<StayResponse> remark(@PathVariable Long id, @RequestBody StayRemarkRequest request) {
        return R.ok(stayService.updateRemark(id, request));
    }

    /**
     * 在住提前退房（退订退款）。
     *
     * @param id      在住单 ID
     * @param request 请求体
     * @return 已退房详情
     */
    @PostMapping("/{id}/void-checkout")
    @PreAuthorize("hasAuthority('billing:checkout')")
    public R<StayResponse> voidCheckout(@PathVariable Long id, @Validated @RequestBody VoidCheckoutRequest request) {
        return R.ok(stayService.voidCheckout(id, request));
    }

    /**
     * 退房（仅释放客房，入住时已结账）。
     *
     * @param id 在住单 ID
     * @return 已退房详情
     */
    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('billing:checkout')")
    public R<StayResponse> checkout(@PathVariable Long id) {
        return R.ok(checkoutService.checkout(id));
    }
}
