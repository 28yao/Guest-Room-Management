package com.hotel.grms.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.billing.FolioStatus;
import com.hotel.grms.module.billing.PaymentMethod;
import com.hotel.grms.module.billing.entity.Folio;
import com.hotel.grms.module.billing.entity.FolioLine;
import com.hotel.grms.module.billing.entity.Payment;
import com.hotel.grms.module.billing.mapper.FolioLineMapper;
import com.hotel.grms.module.billing.mapper.FolioMapper;
import com.hotel.grms.module.billing.mapper.PaymentMapper;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.service.RoomTypeService;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 账单服务：创建账单、按晚生成房费行、整段重算（换房）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class BillingService {

    private static final String LINE_TYPE_ROOM = "ROOM";

    private static final Set<String> ALLOWED_METHODS = new HashSet<String>(Arrays.asList(
            PaymentMethod.CASH, PaymentMethod.WECHAT, PaymentMethod.ALIPAY));

    private final FolioMapper folioMapper;
    private final FolioLineMapper folioLineMapper;
    private final PaymentMapper paymentMapper;
    private final StayOrderMapper stayOrderMapper;
    private final RoomTypeService roomTypeService;

    public BillingService(FolioMapper folioMapper, FolioLineMapper folioLineMapper, PaymentMapper paymentMapper,
                          StayOrderMapper stayOrderMapper, RoomTypeService roomTypeService) {
        this.folioMapper = folioMapper;
        this.folioLineMapper = folioLineMapper;
        this.paymentMapper = paymentMapper;
        this.stayOrderMapper = stayOrderMapper;
        this.roomTypeService = roomTypeService;
    }

    /**
     * 为在住单创建空账单并生成按晚房费明细。
     *
     * @param stayOrder 在住单
     * @return 账单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long initFolioForStay(StayOrder stayOrder) {
        Folio folio = new Folio();
        folio.setStayOrderId(stayOrder.getId());
        folio.setTotalAmount(BigDecimal.ZERO);
        folio.setPaidAmount(BigDecimal.ZERO);
        folio.setStatus(FolioStatus.OPEN);
        folioMapper.insert(folio);
        regenerateActiveLines(folio.getId(), stayOrder);
        return folio.getId();
    }

    /**
     * 按当前在住单房型与房价整段重算账单（换房后调用，BR-06）。
     *
     * @param stayOrderId 在住单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recalculateFullStay(Long stayOrderId) {
        StayOrder stayOrder = stayOrderMapper.selectById(stayOrderId);
        if (stayOrder == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        Folio folio = findFolioByStay(stayOrderId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        regenerateActiveLines(folio.getId(), stayOrder);
    }

    private void regenerateActiveLines(Long folioId, StayOrder stayOrder) {
        folioLineMapper.update(null, new LambdaUpdateWrapper<FolioLine>()
                .eq(FolioLine::getFolioId, folioId)
                .eq(FolioLine::getActive, 1)
                .set(FolioLine::getActive, 0));

        BigDecimal dailyRate = resolveDailyRate(stayOrder);
        int nights = computeNights(stayOrder.getArrivalDate(), stayOrder.getDepartureDate());
        BigDecimal total = BigDecimal.ZERO;
        List<FolioLine> lines = new ArrayList<FolioLine>();
        LocalDate nightDate = stayOrder.getArrivalDate();
        for (int index = 0; index < nights; index++) {
            FolioLine line = new FolioLine();
            line.setFolioId(folioId);
            line.setLineType(LINE_TYPE_ROOM);
            line.setDescription("房费 " + nightDate);
            line.setQuantity(1);
            line.setUnitPrice(dailyRate);
            line.setAmount(dailyRate);
            line.setActive(1);
            lines.add(line);
            total = total.add(dailyRate);
            nightDate = nightDate.plusDays(1);
        }
        for (FolioLine line : lines) {
            folioLineMapper.insert(line);
        }
        Folio update = new Folio();
        update.setId(folioId);
        update.setTotalAmount(total);
        folioMapper.updateById(update);
    }

    private BigDecimal resolveDailyRate(StayOrder stayOrder) {
        if (stayOrder.getAgreedDailyRate() != null) {
            return stayOrder.getAgreedDailyRate();
        }
        RoomType type = roomTypeService.getById(stayOrder.getRoomTypeId());
        return type.getRackRate();
    }

    private int computeNights(LocalDate arrival, LocalDate departure) {
        long nights = ChronoUnit.DAYS.between(arrival, departure);
        if (nights < 1) {
            nights = 1;
        }
        return (int) nights;
    }

    private Folio findFolioByStay(Long stayOrderId) {
        return folioMapper.selectOne(new LambdaQueryWrapper<Folio>().eq(Folio::getStayOrderId, stayOrderId));
    }

    /**
     * 按计费截止日截断在住账单房费行并重算合计。
     *
     * @param stayOrderId       在住单 ID
     * @param chargeThroughDate 计至日期（含该晚）
     * @return 截断后账单
     */
    @Transactional(rollbackFor = Exception.class)
    public Folio truncateFolioToChargeDate(Long stayOrderId, LocalDate chargeThroughDate) {
        StayOrder stayOrder = stayOrderMapper.selectById(stayOrderId);
        if (stayOrder == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        Folio folio = findFolioByStay(stayOrderId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        regenerateActiveLinesUpTo(folio.getId(), stayOrder, chargeThroughDate);
        return folioMapper.selectById(folio.getId());
    }

    /**
     * 记录在住账单退款（负向金额计入当班）。
     *
     * @param folioId         账单 ID
     * @param refundAmount    退款金额（正数）
     * @param refundMethod    退款方式
     * @param shiftSessionId  当班 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordStayRefund(Long folioId, BigDecimal refundAmount, String refundMethod, Long shiftSessionId) {
        assertRefundMethod(refundMethod);
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Folio folio = folioMapper.selectById(folioId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        insertRefundPayment(folioId, refundAmount, refundMethod, shiftSessionId);
        BigDecimal newPaid = folio.getPaidAmount().subtract(refundAmount);
        if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
            newPaid = BigDecimal.ZERO;
        }
        Folio update = new Folio();
        update.setId(folioId);
        update.setPaidAmount(newPaid);
        folioMapper.updateById(update);
    }

    /**
     * 记录预订退订退款（无账单关联）。
     *
     * @param refundAmount   退款金额
     * @param refundMethod   退款方式
     * @param shiftSessionId 当班 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordReservationRefund(BigDecimal refundAmount, String refundMethod, Long shiftSessionId) {
        assertRefundMethod(refundMethod);
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        insertRefundPayment(null, refundAmount, refundMethod, shiftSessionId);
    }

    /**
     * 关闭在住账单。
     *
     * @param folioId 账单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeFolio(Long folioId) {
        Folio update = new Folio();
        update.setId(folioId);
        update.setStatus(FolioStatus.CLOSED);
        folioMapper.updateById(update);
    }

    private void regenerateActiveLinesUpTo(Long folioId, StayOrder stayOrder, LocalDate chargeThroughDate) {
        folioLineMapper.update(null, new LambdaUpdateWrapper<FolioLine>()
                .eq(FolioLine::getFolioId, folioId)
                .eq(FolioLine::getActive, 1)
                .set(FolioLine::getActive, 0));

        int maxNights = computeNights(stayOrder.getArrivalDate(), stayOrder.getDepartureDate());
        int chargedNights = computeChargedNights(stayOrder.getArrivalDate(), stayOrder.getDepartureDate(),
                chargeThroughDate);
        if (chargedNights > maxNights) {
            chargedNights = maxNights;
        }

        BigDecimal dailyRate = resolveDailyRate(stayOrder);
        BigDecimal total = BigDecimal.ZERO;
        List<FolioLine> lines = new ArrayList<FolioLine>();
        LocalDate nightDate = stayOrder.getArrivalDate();
        for (int index = 0; index < chargedNights; index++) {
            FolioLine line = new FolioLine();
            line.setFolioId(folioId);
            line.setLineType(LINE_TYPE_ROOM);
            line.setDescription("房费 " + nightDate);
            line.setQuantity(1);
            line.setUnitPrice(dailyRate);
            line.setAmount(dailyRate);
            line.setActive(1);
            lines.add(line);
            total = total.add(dailyRate);
            nightDate = nightDate.plusDays(1);
        }
        for (FolioLine line : lines) {
            folioLineMapper.insert(line);
        }
        Folio update = new Folio();
        update.setId(folioId);
        update.setTotalAmount(total);
        folioMapper.updateById(update);
    }

    private int computeChargedNights(LocalDate arrival, LocalDate departure, LocalDate chargeThroughDate) {
        LocalDate capped = chargeThroughDate;
        if (capped.isAfter(departure.minusDays(1))) {
            capped = departure.minusDays(1);
        }
        if (capped.isBefore(arrival)) {
            return 1;
        }
        long nights = ChronoUnit.DAYS.between(arrival, capped.plusDays(1));
        if (nights < 1) {
            nights = 1;
        }
        return (int) nights;
    }

    private void insertRefundPayment(Long folioId, BigDecimal refundAmount, String refundMethod, Long shiftSessionId) {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            throw new BusinessException(40101, "未登录");
        }
        Payment payment = new Payment();
        payment.setFolioId(folioId);
        payment.setShiftSessionId(shiftSessionId);
        payment.setMethod(refundMethod);
        payment.setAmount(refundAmount.negate());
        payment.setPaidAt(LocalDateTime.now());
        payment.setOperatorId(operatorId);
        paymentMapper.insert(payment);
    }

    private void assertRefundMethod(String method) {
        if (!StringUtils.hasText(method) || !ALLOWED_METHODS.contains(method)) {
            throw new BusinessException(40022, "退款方式须为 CASH、WECHAT 或 ALIPAY");
        }
    }
}
