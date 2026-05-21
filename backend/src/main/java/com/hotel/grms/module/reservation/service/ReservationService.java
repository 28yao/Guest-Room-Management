package com.hotel.grms.module.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.common.PageResult;
import com.hotel.grms.module.reservation.ReservationStatus;
import com.hotel.grms.module.billing.service.BillingService;
import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.CancelWithRefundRequest;
import com.hotel.grms.module.reservation.dto.ReleaseReservationRequest;
import com.hotel.grms.module.shift.service.ShiftSessionService;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.reservation.dto.ReservationResponse;
import com.hotel.grms.module.reservation.dto.ReservationUpdateRequest;
import com.hotel.grms.module.reservation.entity.Reservation;
import com.hotel.grms.module.reservation.mapper.ReservationMapper;
import com.hotel.grms.module.reservation.support.ReservationTimePolicy;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.room.service.RoomService;
import com.hotel.grms.module.room.service.RoomTypeService;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预订创建、查询、改期、预排房、取消与释放服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class ReservationService {

    private static final DateTimeFormatter RES_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReservationMapper reservationMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final BillingService billingService;
    private final ShiftSessionService shiftSessionService;

    public ReservationService(ReservationMapper reservationMapper, RoomMapper roomMapper,
                              RoomService roomService, RoomTypeService roomTypeService,
                              RoomAvailabilityService roomAvailabilityService,
                              BillingService billingService, ShiftSessionService shiftSessionService) {
        this.reservationMapper = reservationMapper;
        this.roomMapper = roomMapper;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.roomAvailabilityService = roomAvailabilityService;
        this.billingService = billingService;
        this.shiftSessionService = shiftSessionService;
    }

    /**
     * 创建预订，初始状态为已确认。
     *
     * @param request 请求
     * @return 预订详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse create(ReservationCreateRequest request) {
        roomTypeService.getById(request.getRoomTypeId());
        Reservation entity = new Reservation();
        entity.setResNo(generateResNo());
        entity.setGuestName(request.getGuestName());
        entity.setGuestPhone(request.getGuestPhone());
        entity.setRoomTypeId(request.getRoomTypeId());
        applyDateTimes(entity, request.getArrivalDate(), request.getDepartureDate(),
                request.getArrivalAt(), request.getDepartureAt());
        entity.setStatus(ReservationStatus.CONFIRMED);
        entity.setRemark(request.getRemark());
        Long userId = SecurityUtils.currentUserId();
        entity.setCreatedBy(userId == null ? 0L : userId);
        reservationMapper.insert(entity);
        return toResponse(reservationMapper.selectById(entity.getId()));
    }

    /**
     * 分页查询预订列表。
     *
     * @param status      状态筛选
     * @param arrivalFrom 入住日起
     * @param arrivalTo   入住日止
     * @param guestPhone  手机号模糊
     * @param guestName   客人姓名模糊
     * @param page        页码
     * @param size        每页条数
     * @return 分页结果
     */
    public PageResult<ReservationResponse> list(String status, LocalDate arrivalFrom, LocalDate arrivalTo,
                                                String guestPhone, String guestName, int page, int size) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .orderByDesc(Reservation::getArrivalDate)
                .orderByDesc(Reservation::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Reservation::getStatus, status);
        }
        if (arrivalFrom != null) {
            wrapper.ge(Reservation::getArrivalDate, arrivalFrom);
        }
        if (arrivalTo != null) {
            wrapper.le(Reservation::getArrivalDate, arrivalTo);
        }
        if (StringUtils.hasText(guestPhone)) {
            wrapper.like(Reservation::getGuestPhone, guestPhone);
        }
        if (StringUtils.hasText(guestName)) {
            wrapper.like(Reservation::getGuestName, guestName);
        }
        Page<Reservation> pageQuery = new Page<Reservation>(page, size);
        Page<Reservation> result = reservationMapper.selectPage(pageQuery, wrapper);
        List<ReservationResponse> records = new ArrayList<ReservationResponse>();
        for (Reservation item : result.getRecords()) {
            records.add(toResponse(item));
        }
        return new PageResult<ReservationResponse>(result.getTotal(), records);
    }

    /**
     * 按 ID 查询预订。
     *
     * @param id 预订 ID
     * @return 详情
     */
    public ReservationResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    /**
     * 更新预订信息。
     *
     * @param id      预订 ID
     * @param request 请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        Reservation entity = getEntity(id);
        assertEditable(entity);
        roomTypeService.getById(request.getRoomTypeId());
        applyDateTimes(entity, request.getArrivalDate(), request.getDepartureDate(),
                request.getArrivalAt(), request.getDepartureAt());
        if (entity.getRoomId() != null) {
            roomAvailabilityService.assertAssignable(entity.getRoomId(), entity.getArrivalAt(),
                    entity.getDepartureAt(), id);
        }
        entity.setGuestName(request.getGuestName());
        entity.setGuestPhone(request.getGuestPhone());
        entity.setRoomTypeId(request.getRoomTypeId());
        entity.setRemark(request.getRemark());
        reservationMapper.updateById(entity);
        return toResponse(reservationMapper.selectById(id));
    }

    /**
     * 预排房。
     *
     * @param id      预订 ID
     * @param request 排房请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse assignRoom(Long id, AssignRoomRequest request) {
        Reservation entity = getEntity(id);
        assertAssignableReservation(entity);
        Room target = roomService.getById(request.getRoomId());
        if (!target.getRoomTypeId().equals(entity.getRoomTypeId())) {
            throw new BusinessException(40016, "客房房型与预订房型不一致");
        }
        roomAvailabilityService.assertAssignable(request.getRoomId(), entity.getArrivalAt(),
                entity.getDepartureAt(), id);
        releaseAssignedRoomIfNeeded(entity);
        roomService.transitionOccupancy(request.getRoomId(), RoomStatus.RESERVED, null);
        entity.setRoomId(request.getRoomId());
        reservationMapper.updateById(entity);
        return toResponse(reservationMapper.selectById(id));
    }

    /**
     * 取消预订。
     *
     * @param id 预订 ID
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse cancel(Long id) {
        Reservation entity = getEntity(id);
        assertCancellable(entity);
        releaseAssignedRoomIfNeeded(entity);
        entity.setStatus(ReservationStatus.CANCELLED);
        reservationMapper.updateById(entity);
        return toResponse(reservationMapper.selectById(id));
    }

    /**
     * 退订并退款：取消预订、释放房态，可选记录退款流水。
     *
     * @param id      预订 ID
     * @param request 退款请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse cancelWithRefund(Long id, CancelWithRefundRequest request) {
        Reservation entity = getEntity(id);
        assertCancellable(entity);
        releaseAssignedRoomIfNeeded(entity);
        entity.setStatus(ReservationStatus.CANCELLED);
        if (request != null && StringUtils.hasText(request.getRemark())) {
            String existing = entity.getRemark();
            entity.setRemark(StringUtils.hasText(existing) ? existing + "；" + request.getRemark() : request.getRemark());
        }
        reservationMapper.updateById(entity);
        if (request != null) {
            BigDecimal refund = request.getRefundAmount() != null ? request.getRefundAmount() : BigDecimal.ZERO;
            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                Long shiftId = shiftSessionService.requireOpenSessionId();
                billingService.recordReservationRefund(refund, request.getRefundMethod(), shiftId);
            }
        }
        return toResponse(reservationMapper.selectById(id));
    }

    /**
     * 手动释放预订。
     *
     * @param id      预订 ID
     * @param request 释放请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationResponse release(Long id, ReleaseReservationRequest request) {
        Reservation entity = getEntity(id);
        assertReleasable(entity);
        releaseAssignedRoomIfNeeded(entity);
        boolean noShow = request != null && Boolean.TRUE.equals(request.getNoShow());
        entity.setStatus(noShow ? ReservationStatus.NO_SHOW : ReservationStatus.RELEASED);
        reservationMapper.updateById(entity);
        return toResponse(reservationMapper.selectById(id));
    }

    private Reservation getEntity(Long id) {
        Reservation entity = reservationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(40017, "预订不存在");
        }
        return entity;
    }

    private void assertEditable(Reservation entity) {
        if (ReservationStatus.CHECKED_IN.equals(entity.getStatus())) {
            throw new BusinessException(40018, "已入住预订不可修改");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BusinessException(40018, "当前状态不可修改预订");
        }
    }

    private void assertAssignableReservation(Reservation entity) {
        if (!ReservationStatus.CONFIRMED.equals(entity.getStatus())
                && !ReservationStatus.PENDING.equals(entity.getStatus())) {
            throw new BusinessException(40018, "仅待确认或已确认预订可预排房");
        }
    }

    private void assertCancellable(Reservation entity) {
        if (ReservationStatus.CHECKED_IN.equals(entity.getStatus())) {
            throw new BusinessException(40018, "已入住预订不可取消");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BusinessException(40018, "当前状态不可取消");
        }
    }

    private void assertReleasable(Reservation entity) {
        if (ReservationStatus.CHECKED_IN.equals(entity.getStatus())) {
            throw new BusinessException(40018, "已入住预订不可释放");
        }
        if (isTerminal(entity.getStatus())) {
            throw new BusinessException(40018, "当前状态不可释放");
        }
    }

    private boolean isTerminal(String status) {
        return ReservationStatus.CANCELLED.equals(status)
                || ReservationStatus.NO_SHOW.equals(status)
                || ReservationStatus.RELEASED.equals(status);
    }

    private void releaseAssignedRoomIfNeeded(Reservation entity) {
        Long roomId = entity.getRoomId();
        if (roomId == null) {
            return;
        }
        Room room = roomMapper.selectById(roomId);
        if (room != null && RoomStatus.RESERVED.equals(room.getStatus())) {
            roomService.transitionOccupancy(roomId, RoomStatus.VACANT, null);
        }
        entity.setRoomId(null);
    }

    private void applyDateTimes(Reservation entity, LocalDate arrivalDate, LocalDate departureDate,
                                LocalDateTime arrivalAt, LocalDateTime departureAt) {
        LocalDateTime[] range = ReservationTimePolicy.resolveRange(arrivalDate, departureDate, arrivalAt, departureAt);
        entity.setArrivalAt(range[0]);
        entity.setDepartureAt(range[1]);
        entity.setArrivalDate(range[0].toLocalDate());
        entity.setDepartureDate(range[1].toLocalDate());
    }

    private String generateResNo() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "R" + java.time.LocalDateTime.now().format(RES_NO_TIME) + suffix;
    }

    private ReservationResponse toResponse(Reservation entity) {
        ReservationResponse response = new ReservationResponse();
        response.setId(entity.getId());
        response.setResNo(entity.getResNo());
        response.setGuestName(entity.getGuestName());
        response.setGuestPhone(entity.getGuestPhone());
        response.setRoomTypeId(entity.getRoomTypeId());
        response.setRoomId(entity.getRoomId());
        response.setArrivalDate(entity.getArrivalDate());
        response.setDepartureDate(entity.getDepartureDate());
        response.setArrivalAt(entity.getArrivalAt());
        response.setDepartureAt(entity.getDepartureAt());
        response.setStatus(entity.getStatus());
        response.setRemark(entity.getRemark());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        if (entity.getRoomTypeId() != null) {
            RoomType type = roomTypeService.getById(entity.getRoomTypeId());
            response.setRoomTypeName(type.getName());
            response.setRackRate(type.getRackRate());
        }
        if (entity.getRoomId() != null) {
            Room room = roomMapper.selectById(entity.getRoomId());
            if (room != null) {
                response.setRoomNo(room.getRoomNo());
            }
        }
        return response;
    }
}
