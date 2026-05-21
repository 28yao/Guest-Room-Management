package com.hotel.grms.module.stay.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.audit.support.AuditBizType;
import com.hotel.grms.module.audit.support.AuditContextHolder;
import com.hotel.grms.module.audit.support.AuditJsonHelper;
import com.hotel.grms.module.audit.support.AuditOpType;
import com.hotel.grms.module.audit.support.AuditedOperation;
import com.hotel.grms.module.billing.service.BillingService;
import com.hotel.grms.module.reservation.ReservationStatus;
import com.hotel.grms.module.reservation.entity.Reservation;
import com.hotel.grms.module.reservation.mapper.ReservationMapper;
import com.hotel.grms.module.reservation.service.RoomAvailabilityService;
import com.hotel.grms.module.reservation.support.ReservationTimePolicy;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.service.RoomService;
import com.hotel.grms.module.room.service.RoomTypeService;
import com.hotel.grms.module.room.state.RoomStateMachine;
import com.hotel.grms.module.shift.service.ShiftSessionService;
import com.hotel.grms.module.stay.StayStatus;
import com.hotel.grms.module.billing.entity.Folio;
import com.hotel.grms.module.billing.mapper.FolioMapper;
import com.hotel.grms.module.stay.dto.ChangeRoomRequest;
import com.hotel.grms.module.stay.dto.CheckInFromReservationRequest;
import com.hotel.grms.module.stay.dto.StayRemarkRequest;
import com.hotel.grms.module.stay.dto.StayResponse;
import com.hotel.grms.module.stay.dto.VoidCheckoutRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import com.hotel.grms.module.stay.entity.StayGuest;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayGuestMapper;
import com.hotel.grms.module.stay.mapper.StayInHouseRow;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
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
 * 入住与在住服务：Walk-in、预订入住、在住列表、换房、备注。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class StayService {

    private static final DateTimeFormatter STAY_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final StayOrderMapper stayOrderMapper;
    private final StayGuestMapper stayGuestMapper;
    private final ReservationMapper reservationMapper;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final RoomStateMachine roomStateMachine;
    private final RoomAvailabilityService roomAvailabilityService;
    private final ShiftSessionService shiftSessionService;
    private final BillingService billingService;
    private final FolioMapper folioMapper;
    private final AuditJsonHelper auditJsonHelper;

    public StayService(StayOrderMapper stayOrderMapper, StayGuestMapper stayGuestMapper,
                       ReservationMapper reservationMapper, RoomService roomService,
                       RoomTypeService roomTypeService, RoomStateMachine roomStateMachine,
                       RoomAvailabilityService roomAvailabilityService,
                       ShiftSessionService shiftSessionService, BillingService billingService,
                       FolioMapper folioMapper, AuditJsonHelper auditJsonHelper) {
        this.stayOrderMapper = stayOrderMapper;
        this.stayGuestMapper = stayGuestMapper;
        this.reservationMapper = reservationMapper;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.roomStateMachine = roomStateMachine;
        this.roomAvailabilityService = roomAvailabilityService;
        this.shiftSessionService = shiftSessionService;
        this.billingService = billingService;
        this.folioMapper = folioMapper;
        this.auditJsonHelper = auditJsonHelper;
    }

    /**
     * Walk-in 入住。
     *
     * @param request 请求
     * @return 在住详情
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.STAY, operationType = AuditOpType.STAY_CHECK_IN)
    public StayResponse checkInWalkIn(WalkInCheckInRequest request) {
        Long shiftId = shiftSessionService.requireOpenSessionId();
        assertDateRange(request.getArrivalDate(), request.getDepartureDate());
        Room room = roomService.getById(request.getRoomId());
        roomStateMachine.assertCheckInAllowed(room);
        LocalDateTime[] range = ReservationTimePolicy.resolveRange(request.getArrivalDate(),
                request.getDepartureDate(), request.getArrivalAt(), request.getDepartureAt());
        roomAvailabilityService.assertNoStayTimeConflict(request.getRoomId(), range[0], range[1], null);
        roomAvailabilityService.assertAssignable(request.getRoomId(), range[0], range[1], null);
        StayOrder stay = buildStayOrder(null, room, request.getArrivalDate(), request.getDepartureDate(),
                request.getAgreedDailyRate(), request.getRemark(),
                request.getGuestName(), request.getGuestPhone());
        insertGuest(stay.getId(), request.getGuestName(), request.getGuestPhone(), request.getIdCard());
        Long folioId = billingService.initFolioForStay(stay);
        billingService.settleFolioAtCheckIn(folioId, request.getPayments(), shiftId);
        roomService.transitionOccupancy(room.getId(), RoomStatus.OCCUPIED, null);
        StayResponse response = getById(stay.getId());
        AuditContextHolder.bind(stay.getId(), null,
                auditJsonHelper.pairs("stayNo", response.getStayNo(), "roomNo", response.getRoomNo(),
                        "guestName", response.getGuestName()),
                "Walk-in 入住");
        return response;
    }

    /**
     * 预订入住。
     *
     * @param request 请求
     * @return 在住详情
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.STAY, operationType = AuditOpType.STAY_CHECK_IN_RES)
    public StayResponse checkInFromReservation(CheckInFromReservationRequest request) {
        Long shiftId = shiftSessionService.requireOpenSessionId();
        Reservation reservation = reservationMapper.selectById(request.getReservationId());
        if (reservation == null) {
            throw new BusinessException(40012, "预订不存在");
        }
        if (!ReservationStatus.CONFIRMED.equals(reservation.getStatus())) {
            throw new BusinessException(40017, "仅已确认预订可办理入住");
        }
        Long roomId = request.getRoomId() != null ? request.getRoomId() : reservation.getRoomId();
        if (roomId == null) {
            throw new BusinessException(40018, "请先预排房或指定入住客房");
        }
        Room room = roomService.getById(roomId);
        if (!room.getRoomTypeId().equals(reservation.getRoomTypeId())) {
            throw new BusinessException(40016, "客房房型与预订房型不一致");
        }
        roomStateMachine.assertCheckInAllowed(room);
        roomAvailabilityService.assertNoStayTimeConflict(roomId, reservation.getArrivalAt(),
                reservation.getDepartureAt(), null);
        if (request.getRoomId() != null && !request.getRoomId().equals(reservation.getRoomId())) {
            roomAvailabilityService.assertAssignable(roomId, reservation.getArrivalAt(),
                    reservation.getDepartureAt(), reservation.getId());
        }
        java.math.BigDecimal agreedRate = request.getAgreedDailyRate();
        if (agreedRate == null) {
            agreedRate = roomTypeService.getById(room.getRoomTypeId()).getRackRate();
        }
        StayOrder stay = buildStayOrder(reservation.getId(), room, reservation.getArrivalDate(),
                reservation.getDepartureDate(), agreedRate, request.getRemark(),
                reservation.getGuestName(), reservation.getGuestPhone());
        insertGuest(stay.getId(), reservation.getGuestName(), reservation.getGuestPhone(), null);
        Long folioId = billingService.initFolioForStay(stay);
        billingService.settleFolioAtCheckIn(folioId, request.getPayments(), shiftId);
        roomService.transitionOccupancy(room.getId(), RoomStatus.OCCUPIED, null);
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setRoomId(roomId);
        reservationMapper.updateById(reservation);
        StayResponse response = getById(stay.getId());
        AuditContextHolder.bind(stay.getId(), null,
                auditJsonHelper.pairs("stayNo", response.getStayNo(), "resNo", response.getResNo(),
                        "roomNo", response.getRoomNo()),
                "预订入住");
        return response;
    }

    /**
     * 在住列表。
     *
     * @return 在住列表
     */
    public List<StayResponse> listInHouse(String guestName) {
        String keyword = StringUtils.hasText(guestName) ? guestName.trim() : null;
        List<StayInHouseRow> rows = stayOrderMapper.selectInHouseRows(keyword);
        List<StayResponse> result = new ArrayList<StayResponse>(rows.size());
        for (StayInHouseRow row : rows) {
            result.add(toResponse(row));
        }
        return result;
    }

    /**
     * 按 ID 查询在住单。
     *
     * @param id 在住单 ID
     * @return 详情
     */
    public StayResponse getById(Long id) {
        StayOrder stay = getEntity(id);
        StayGuest guest = stayGuestMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StayGuest>()
                        .eq(StayGuest::getStayOrderId, id).last("LIMIT 1"));
        Room room = roomService.getById(stay.getRoomId());
        RoomType type = roomTypeService.getById(stay.getRoomTypeId());
        StayResponse response = new StayResponse();
        response.setId(stay.getId());
        response.setStayNo(stay.getStayNo());
        response.setReservationId(stay.getReservationId());
        response.setRoomId(stay.getRoomId());
        response.setRoomNo(room.getRoomNo());
        response.setRoomVersion(room.getVersion());
        response.setRoomTypeId(stay.getRoomTypeId());
        response.setRoomTypeName(type.getName());
        if (guest != null) {
            response.setGuestName(guest.getGuestName());
            response.setGuestPhone(guest.getGuestPhone());
            response.setIdCard(guest.getIdCard());
        }
        response.setArrivalDate(stay.getArrivalDate());
        response.setDepartureDate(stay.getDepartureDate());
        response.setAgreedDailyRate(stay.getAgreedDailyRate());
        response.setStatus(stay.getStatus());
        response.setRemark(stay.getRemark());
        response.setCheckInAt(stay.getCheckInAt());
        if (stay.getReservationId() != null) {
            Reservation res = reservationMapper.selectById(stay.getReservationId());
            if (res != null) {
                response.setResNo(res.getResNo());
            }
        }
        Folio folio = folioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Folio>()
                        .eq(Folio::getStayOrderId, id).last("LIMIT 1"));
        if (folio != null) {
            response.setFolioId(folio.getId());
            response.setFolioTotalAmount(folio.getTotalAmount());
            response.setFolioPaidAmount(folio.getPaidAmount());
        }
        return response;
    }

    /**
     * 换房并整段重算账单。
     *
     * @param id      在住单 ID
     * @param request 请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.STAY, operationType = AuditOpType.STAY_CHANGE_ROOM)
    public StayResponse changeRoom(Long id, ChangeRoomRequest request) {
        StayOrder stay = getEntity(id);
        assertInHouse(stay);
        if (stay.getRoomId().equals(request.getTargetRoomId())) {
            throw new BusinessException(40019, "目标客房与当前客房相同");
        }
        Room oldRoom = roomService.getById(stay.getRoomId());
        String beforeJson = auditJsonHelper.pairs("roomNo", oldRoom.getRoomNo(), "roomId", stay.getRoomId());
        Room target = roomService.getById(request.getTargetRoomId());
        roomStateMachine.assertCheckInAllowed(target);
        LocalDateTime stayStart = ReservationTimePolicy.effectiveStayStart(stay);
        LocalDateTime stayEnd = ReservationTimePolicy.effectiveStayEnd(stay);
        roomAvailabilityService.assertNoStayTimeConflict(request.getTargetRoomId(), stayStart, stayEnd, id);
        roomAvailabilityService.assertAssignable(request.getTargetRoomId(), stayStart, stayEnd, null);
        Long oldRoomId = stay.getRoomId();
        roomService.transitionOccupancy(oldRoomId, RoomStatus.VACANT, null);
        roomService.markDirty(oldRoomId, null);
        roomService.transitionOccupancy(request.getTargetRoomId(), RoomStatus.OCCUPIED,
                request.getTargetRoomVersion());
        stay.setRoomId(request.getTargetRoomId());
        stay.setRoomTypeId(target.getRoomTypeId());
        stayOrderMapper.updateById(stay);
        billingService.recalculateFullStay(id);
        StayResponse response = getById(id);
        AuditContextHolder.bind(id, beforeJson,
                auditJsonHelper.pairs("roomNo", response.getRoomNo(), "roomId", response.getRoomId()),
                "换房");
        return response;
    }

    /**
     * 更新在住备注。
     *
     * @param id      在住单 ID
     * @param request 请求
     * @return 更新后详情
     */
    @Transactional(rollbackFor = Exception.class)
    public StayResponse updateRemark(Long id, StayRemarkRequest request) {
        StayOrder stay = getEntity(id);
        assertInHouse(stay);
        stay.setRemark(request.getRemark());
        stayOrderMapper.updateById(stay);
        return getById(id);
    }

    /**
     * 在住提前退房（退订退款）：截断房费、可选退款、关账、客房置脏。
     *
     * @param id      在住单 ID
     * @param request 请求
     * @return 已退房详情（状态 CHECKED_OUT）
     */
    @Transactional(rollbackFor = Exception.class)
    public StayResponse voidCheckout(Long id, VoidCheckoutRequest request) {
        Long shiftId = shiftSessionService.requireOpenSessionId();
        StayOrder stay = getEntity(id);
        assertInHouse(stay);
        LocalDate chargeThrough = request.getChargeThroughDate() != null
                ? request.getChargeThroughDate() : LocalDate.now();
        if (chargeThrough.isBefore(stay.getArrivalDate())) {
            throw new BusinessException(40023, "计费截止日不能早于入住日期");
        }
        Folio folio = billingService.truncateFolioToChargeDate(id, chargeThrough);
        BigDecimal refund = resolveRefundAmount(folio, request.getRefundAmount());
        if (folio.getId() != null && refund.compareTo(BigDecimal.ZERO) > 0) {
            billingService.recordStayRefund(folio.getId(), refund, request.getRefundMethod(), shiftId);
        }
        billingService.closeFolio(folio.getId());
        stay.setStatus(StayStatus.CHECKED_OUT);
        stay.setCheckOutAt(LocalDateTime.now());
        if (StringUtils.hasText(request.getRemark())) {
            String existing = stay.getRemark();
            stay.setRemark(StringUtils.hasText(existing) ? existing + "；" + request.getRemark() : request.getRemark());
        }
        stayOrderMapper.updateById(stay);
        if (stay.getReservationId() != null) {
            Reservation reservation = reservationMapper.selectById(stay.getReservationId());
            if (reservation != null && ReservationStatus.CHECKED_IN.equals(reservation.getStatus())) {
                reservation.setStatus(ReservationStatus.RELEASED);
                reservationMapper.updateById(reservation);
            }
        }
        roomService.transitionOccupancy(stay.getRoomId(), RoomStatus.VACANT, null);
        roomService.markDirty(stay.getRoomId(), null);
        return getById(id);
    }

    private BigDecimal resolveRefundAmount(Folio folio, BigDecimal requested) {
        BigDecimal paid = folio.getPaidAmount() != null ? folio.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal total = folio.getTotalAmount() != null ? folio.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal auto = paid.subtract(total);
        if (auto.compareTo(BigDecimal.ZERO) < 0) {
            auto = BigDecimal.ZERO;
        }
        if (requested == null) {
            return auto;
        }
        if (requested.compareTo(paid) > 0) {
            throw new BusinessException(40024, "退款金额不能大于已收金额");
        }
        return requested;
    }

    private StayOrder buildStayOrder(Long reservationId, Room room, LocalDate arrival, LocalDate departure,
                                     java.math.BigDecimal agreedRate, String remark,
                                     String guestName, String guestPhone) {
        assertDateRange(arrival, departure);
        java.math.BigDecimal dailyRate = agreedRate;
        if (dailyRate == null) {
            dailyRate = roomTypeService.getById(room.getRoomTypeId()).getRackRate();
        }
        StayOrder stay = new StayOrder();
        stay.setStayNo(generateStayNo());
        stay.setReservationId(reservationId);
        stay.setRoomId(room.getId());
        stay.setRoomTypeId(room.getRoomTypeId());
        stay.setGuestName(guestName);
        stay.setGuestPhone(guestPhone);
        stay.setArrivalDate(arrival);
        stay.setDepartureDate(departure);
        stay.setAgreedDailyRate(dailyRate);
        stay.setStatus(StayStatus.IN_HOUSE);
        stay.setRemark(remark);
        stay.setCheckInAt(LocalDateTime.now());
        stayOrderMapper.insert(stay);
        return stayOrderMapper.selectById(stay.getId());
    }

    private void insertGuest(Long stayId, String name, String phone, String idCard) {
        StayGuest guest = new StayGuest();
        guest.setStayOrderId(stayId);
        guest.setGuestName(name);
        guest.setGuestPhone(phone);
        guest.setIdCard(idCard);
        stayGuestMapper.insert(guest);
    }

    private void assertDateRange(LocalDate arrival, LocalDate departure) {
        if (departure == null || arrival == null || !departure.isAfter(arrival)) {
            throw new BusinessException(40020, "离店日期须晚于入住日期");
        }
    }

    private void assertInHouse(StayOrder stay) {
        if (!StayStatus.IN_HOUSE.equals(stay.getStatus())) {
            throw new BusinessException(40021, "仅在住状态可操作");
        }
    }

    private StayOrder getEntity(Long id) {
        StayOrder stay = stayOrderMapper.selectById(id);
        if (stay == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        return stay;
    }

    private String generateStayNo() {
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return "ST" + LocalDateTime.now().format(STAY_NO_TIME) + suffix;
    }

    private StayResponse toResponse(StayInHouseRow row) {
        StayResponse response = new StayResponse();
        response.setId(row.getStayId());
        response.setStayNo(row.getStayNo());
        response.setReservationId(row.getReservationId());
        response.setResNo(row.getResNo());
        response.setRoomId(row.getRoomId());
        response.setRoomNo(row.getRoomNo());
        response.setRoomVersion(row.getRoomVersion());
        response.setRoomTypeId(row.getRoomTypeId());
        response.setRoomTypeName(row.getRoomTypeName());
        response.setGuestName(row.getGuestName());
        response.setGuestPhone(row.getGuestPhone());
        response.setIdCard(row.getIdCard());
        response.setArrivalDate(row.getArrivalDate());
        response.setDepartureDate(row.getDepartureDate());
        response.setAgreedDailyRate(row.getAgreedDailyRate());
        response.setStatus(row.getStatus());
        response.setRemark(row.getRemark());
        response.setCheckInAt(row.getCheckInAt());
        response.setFolioId(row.getFolioId());
        response.setFolioTotalAmount(row.getFolioTotalAmount());
        response.setFolioPaidAmount(row.getFolioPaidAmount());
        return response;
    }
}
