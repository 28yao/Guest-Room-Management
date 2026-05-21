package com.hotel.grms.module.reservation.controller;

import com.hotel.grms.common.PageResult;
import com.hotel.grms.common.R;
import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.AvailableRoomDto;
import com.hotel.grms.module.reservation.dto.CancelWithRefundRequest;
import com.hotel.grms.module.reservation.dto.ReleaseReservationRequest;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.reservation.dto.ReservationResponse;
import com.hotel.grms.module.reservation.dto.ReservationUpdateRequest;
import com.hotel.grms.module.reservation.service.ReservationService;
import com.hotel.grms.module.reservation.service.RoomAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.hotel.grms.module.reservation.support.ReservationTimePolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预订管理 REST 接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final RoomAvailabilityService roomAvailabilityService;

    public ReservationController(ReservationService reservationService,
                                 RoomAvailabilityService roomAvailabilityService) {
        this.reservationService = reservationService;
        this.roomAvailabilityService = roomAvailabilityService;
    }

    /**
     * 创建预订。
     *
     * @param request 请求体
     * @return 新预订
     */
    @PostMapping
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> create(@Validated @RequestBody ReservationCreateRequest request) {
        return R.ok(reservationService.create(request));
    }

    /**
     * 分页查询预订列表。
     *
     * @param status      状态
     * @param arrivalFrom 入住日起
     * @param arrivalTo   入住日止
     * @param guestPhone  手机号
     * @param guestName   客人姓名
     * @param page        页码
     * @param size        每页大小
     * @return 分页数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<PageResult<ReservationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalTo,
            @RequestParam(required = false) String guestPhone,
            @RequestParam(required = false) String guestName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(reservationService.list(status, arrivalFrom, arrivalTo, guestPhone, guestName, page, size));
    }

    /**
     * 查询预订详情。
     *
     * @param id 预订 ID
     * @return 详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> get(@PathVariable Long id) {
        return R.ok(reservationService.getById(id));
    }

    /**
     * 更新预订。
     *
     * @param id      预订 ID
     * @param request 请求体
     * @return 更新后详情
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> update(@PathVariable Long id,
                                         @Validated @RequestBody ReservationUpdateRequest request) {
        return R.ok(reservationService.update(id, request));
    }

    /**
     * 预排房。
     *
     * @param id      预订 ID
     * @param request 排房请求
     * @return 更新后详情
     */
    @PostMapping("/{id}/assign-room")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> assignRoom(@PathVariable Long id,
                                             @Validated @RequestBody AssignRoomRequest request) {
        return R.ok(reservationService.assignRoom(id, request));
    }

    /**
     * 取消预订。
     *
     * @param id 预订 ID
     * @return 更新后详情
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> cancel(@PathVariable Long id) {
        return R.ok(reservationService.cancel(id));
    }

    /**
     * 退订（退款）：取消预订并记录可选退款流水。
     *
     * @param id      预订 ID
     * @param request 请求体
     * @return 更新后详情
     */
    @PostMapping("/{id}/cancel-with-refund")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> cancelWithRefund(@PathVariable Long id,
                                                     @Validated @RequestBody CancelWithRefundRequest request) {
        return R.ok(reservationService.cancelWithRefund(id, request));
    }

    /**
     * 手动释放预订。
     *
     * @param id      预订 ID
     * @param request 释放请求
     * @return 更新后详情
     */
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<ReservationResponse> release(@PathVariable Long id,
                                          @RequestBody(required = false) ReleaseReservationRequest request) {
        return R.ok(reservationService.release(id, request));
    }

    /**
     * 查询可预排房客房。
     *
     * @param roomTypeId           房型 ID
     * @param arrival              入住日
     * @param departure            离店日
     * @param excludeReservationId 排除预订 ID
     * @return 可排房列表
     */
    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public R<List<AvailableRoomDto>> availability(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departure,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureAt,
            @RequestParam(required = false) Long excludeReservationId) {
        LocalDateTime[] range = ReservationTimePolicy.resolveRange(arrival, departure, arrivalAt, departureAt);
        return R.ok(roomAvailabilityService.listAssignableRooms(roomTypeId, range[0], range[1],
                excludeReservationId));
    }
}
