package com.hotel.grms.module.billing.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.audit.support.AuditBizType;
import com.hotel.grms.module.audit.support.AuditContextHolder;
import com.hotel.grms.module.audit.support.AuditJsonHelper;
import com.hotel.grms.module.audit.support.AuditOpType;
import com.hotel.grms.module.audit.support.AuditedOperation;
import com.hotel.grms.module.billing.entity.Folio;
import com.hotel.grms.module.billing.mapper.FolioMapper;
import com.hotel.grms.module.hk.service.HousekeepingService;
import com.hotel.grms.module.reservation.ReservationStatus;
import com.hotel.grms.module.reservation.entity.Reservation;
import com.hotel.grms.module.reservation.mapper.ReservationMapper;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.service.RoomService;
import com.hotel.grms.module.stay.StayStatus;
import com.hotel.grms.module.stay.dto.StayResponse;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
import com.hotel.grms.module.stay.service.StayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 退房服务：仅释放客房（入住时已结账），置脏并生成保洁任务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class CheckoutService {

    private final StayOrderMapper stayOrderMapper;
    private final FolioMapper folioMapper;
    private final BillingService billingService;
    private final RoomService roomService;
    private final HousekeepingService housekeepingService;
    private final ReservationMapper reservationMapper;
    private final StayService stayService;
    private final AuditJsonHelper auditJsonHelper;

    public CheckoutService(StayOrderMapper stayOrderMapper, FolioMapper folioMapper, BillingService billingService,
                         RoomService roomService, HousekeepingService housekeepingService,
                         ReservationMapper reservationMapper, StayService stayService,
                         AuditJsonHelper auditJsonHelper) {
        this.stayOrderMapper = stayOrderMapper;
        this.folioMapper = folioMapper;
        this.billingService = billingService;
        this.roomService = roomService;
        this.housekeepingService = housekeepingService;
        this.reservationMapper = reservationMapper;
        this.stayService = stayService;
        this.auditJsonHelper = auditJsonHelper;
    }

    /**
     * 退房：释放客房，不再次收款（入住时已结清）。
     *
     * @param stayOrderId 在住单 ID
     * @return 已退房详情
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.STAY, operationType = AuditOpType.STAY_CHECKOUT)
    public StayResponse checkout(Long stayOrderId) {
        StayOrder stay = stayOrderMapper.selectById(stayOrderId);
        if (stay == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        if (!StayStatus.IN_HOUSE.equals(stay.getStatus())) {
            throw new BusinessException(40021, "仅在住状态可操作");
        }
        Folio folio = folioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Folio>()
                        .eq(Folio::getStayOrderId, stayOrderId));
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        billingService.assertClosedForCheckout(folio);
        stay.setStatus(StayStatus.CHECKED_OUT);
        stay.setCheckOutAt(LocalDateTime.now());
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
        housekeepingService.createTaskOnDirty(stay.getRoomId());
        StayResponse response = stayService.getById(stayOrderId);
        AuditContextHolder.bind(stayOrderId,
                auditJsonHelper.pairs("status", StayStatus.IN_HOUSE),
                auditJsonHelper.pairs("status", response.getStatus(), "roomNo", response.getRoomNo()),
                "退房释放");
        return response;
    }
}
