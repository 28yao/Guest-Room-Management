package com.hotel.grms.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.billing.FolioStatus;
import com.hotel.grms.module.billing.PaymentMethod;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.billing.dto.FolioDetailResponse;
import com.hotel.grms.module.billing.dto.FolioLineResponse;
import com.hotel.grms.module.billing.entity.Folio;
import com.hotel.grms.module.billing.entity.FolioLine;
import com.hotel.grms.module.billing.entity.Payment;
import com.hotel.grms.module.billing.mapper.FolioLineMapper;
import com.hotel.grms.module.billing.mapper.FolioMapper;
import com.hotel.grms.module.billing.mapper.PaymentMapper;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.service.RoomTypeService;
import com.hotel.grms.module.stay.StayStatus;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.audit.support.AuditBizType;
import com.hotel.grms.module.audit.support.AuditContextHolder;
import com.hotel.grms.module.audit.support.AuditJsonHelper;
import com.hotel.grms.module.audit.support.AuditOpType;
import com.hotel.grms.module.audit.support.AuditedOperation;
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
    private final AuditJsonHelper auditJsonHelper;

    public BillingService(FolioMapper folioMapper, FolioLineMapper folioLineMapper, PaymentMapper paymentMapper,
                          StayOrderMapper stayOrderMapper, RoomTypeService roomTypeService,
                          AuditJsonHelper auditJsonHelper) {
        this.folioMapper = folioMapper;
        this.folioLineMapper = folioLineMapper;
        this.paymentMapper = paymentMapper;
        this.stayOrderMapper = stayOrderMapper;
        this.roomTypeService = roomTypeService;
        this.auditJsonHelper = auditJsonHelper;
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
     * 查询在住单账单详情（含有效明细行）。
     *
     * @param stayOrderId 在住单 ID
     * @return 账单详情
     */
    public FolioDetailResponse getFolioDetailByStay(Long stayOrderId) {
        Folio folio = findFolioByStay(stayOrderId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        return toDetailResponse(folio);
    }

    /**
     * 按账单 ID 查询详情。
     *
     * @param folioId 账单 ID
     * @return 账单详情
     */
    public FolioDetailResponse getFolioDetail(Long folioId) {
        Folio folio = folioMapper.selectById(folioId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        return toDetailResponse(folio);
    }

    /**
     * 改价：更新协议日价并整段重算（BR-05）。
     *
     * @param folioId          账单 ID
     * @param agreedDailyRate  新协议日价
     * @return 更新后账单
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(bizType = AuditBizType.FOLIO, operationType = AuditOpType.FOLIO_ADJUST_PRICE)
    public FolioDetailResponse adjustAgreedDailyRate(Long folioId, BigDecimal agreedDailyRate) {
        Folio folio = folioMapper.selectById(folioId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        StayOrder stayOrder = stayOrderMapper.selectById(folio.getStayOrderId());
        if (stayOrder == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        if (!StayStatus.IN_HOUSE.equals(stayOrder.getStatus())) {
            throw new BusinessException(40025, "仅在住期间可改价");
        }
        String beforeJson = auditJsonHelper.pairs("agreedDailyRate", stayOrder.getAgreedDailyRate(),
                "totalAmount", folio.getTotalAmount());
        if (FolioStatus.CLOSED.equals(folio.getStatus())) {
            Folio reopen = new Folio();
            reopen.setId(folioId);
            reopen.setStatus(FolioStatus.OPEN);
            folioMapper.updateById(reopen);
        }
        stayOrder.setAgreedDailyRate(agreedDailyRate);
        stayOrderMapper.updateById(stayOrder);
        regenerateActiveLines(folioId, stayOrder);
        FolioDetailResponse after = getFolioDetail(folioId);
        String afterJson = auditJsonHelper.pairs("agreedDailyRate", agreedDailyRate,
                "totalAmount", after.getTotalAmount());
        AuditContextHolder.bind(folioId, beforeJson, afterJson, "调整协议日价");
        return after;
    }

    /**
     * 分笔收款，累计已付金额。
     *
     * @param folioId        账单 ID
     * @param amount         收款金额
     * @param method         支付方式
     * @param shiftSessionId 当班 ID
     * @return 更新后账单
     */
    @Transactional(rollbackFor = Exception.class)
    public FolioDetailResponse addPayment(Long folioId, BigDecimal amount, String method, Long shiftSessionId) {
        assertPaymentMethod(method);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(40026, "收款金额须大于 0");
        }
        Folio folio = folioMapper.selectById(folioId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        if (!FolioStatus.OPEN.equals(folio.getStatus())) {
            throw new BusinessException(40027, "仅未结账单可收款");
        }
        insertPositivePayment(folioId, amount, method, shiftSessionId);
        BigDecimal paid = folio.getPaidAmount() != null ? folio.getPaidAmount() : BigDecimal.ZERO;
        Folio update = new Folio();
        update.setId(folioId);
        update.setPaidAmount(paid.add(amount));
        folioMapper.updateById(update);
        return getFolioDetail(folioId);
    }

    /**
     * 入住时结清账单：分笔收款合计须等于应付，并关账（BR-07）。
     *
     * @param folioId        账单 ID
     * @param payments       收款明细
     * @param shiftSessionId 当班 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleFolioAtCheckIn(Long folioId, List<CheckInPaymentItem> payments, Long shiftSessionId) {
        Folio folio = folioMapper.selectById(folioId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        if (!FolioStatus.OPEN.equals(folio.getStatus())) {
            throw new BusinessException(40027, "仅未结账单可收款");
        }
        BigDecimal total = folio.getTotalAmount() != null ? folio.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (CheckInPaymentItem item : payments) {
            assertPaymentMethod(item.getMethod());
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(40026, "收款金额须大于 0");
            }
            insertPositivePayment(folioId, item.getAmount(), item.getMethod(), shiftSessionId);
            sum = sum.add(item.getAmount());
        }
        if (sum.compareTo(total) != 0) {
            throw new BusinessException(40004, "收款合计须等于应付金额，请核对后重试");
        }
        Folio update = new Folio();
        update.setId(folioId);
        update.setPaidAmount(sum);
        folioMapper.updateById(update);
        closeFolio(folioId);
    }

    /**
     * 校验账单已结清（应付=已付）。
     *
     * @param folio 账单
     */
    public void assertSettled(Folio folio) {
        BigDecimal total = folio.getTotalAmount() != null ? folio.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = folio.getPaidAmount() != null ? folio.getPaidAmount() : BigDecimal.ZERO;
        if (total.compareTo(paid) != 0) {
            throw new BusinessException(40004, "账单未结清，请在入住时收齐房费");
        }
    }

    /**
     * 校验账单已关账（入住时已结账）。
     *
     * @param folio 账单
     */
    public void assertClosedForCheckout(Folio folio) {
        if (!FolioStatus.CLOSED.equals(folio.getStatus())) {
            throw new BusinessException(40028, "账单未在入住时结清，无法办理退房");
        }
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
        assertPaymentMethod(method);
    }

    private void assertPaymentMethod(String method) {
        if (!StringUtils.hasText(method) || !ALLOWED_METHODS.contains(method)) {
            throw new BusinessException(40022, "支付方式须为 CASH、WECHAT 或 ALIPAY");
        }
    }

    private void insertPositivePayment(Long folioId, BigDecimal amount, String method, Long shiftSessionId) {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            throw new BusinessException(40101, "未登录");
        }
        Payment payment = new Payment();
        payment.setFolioId(folioId);
        payment.setShiftSessionId(shiftSessionId);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setPaidAt(LocalDateTime.now());
        payment.setOperatorId(operatorId);
        paymentMapper.insert(payment);
    }

    private FolioDetailResponse toDetailResponse(Folio folio) {
        List<FolioLine> activeLines = folioLineMapper.selectList(new LambdaQueryWrapper<FolioLine>()
                .eq(FolioLine::getFolioId, folio.getId())
                .eq(FolioLine::getActive, 1)
                .orderByAsc(FolioLine::getId));
        List<FolioLineResponse> lineResponses = new ArrayList<FolioLineResponse>();
        for (FolioLine line : activeLines) {
            FolioLineResponse row = new FolioLineResponse();
            row.setId(line.getId());
            row.setLineType(line.getLineType());
            row.setDescription(line.getDescription());
            row.setQuantity(line.getQuantity());
            row.setUnitPrice(line.getUnitPrice());
            row.setAmount(line.getAmount());
            lineResponses.add(row);
        }
        BigDecimal total = folio.getTotalAmount() != null ? folio.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = folio.getPaidAmount() != null ? folio.getPaidAmount() : BigDecimal.ZERO;
        FolioDetailResponse response = new FolioDetailResponse();
        response.setId(folio.getId());
        response.setStayOrderId(folio.getStayOrderId());
        response.setTotalAmount(total);
        response.setPaidAmount(paid);
        response.setBalance(total.subtract(paid));
        response.setStatus(folio.getStatus());
        response.setLines(lineResponses);
        return response;
    }
}
