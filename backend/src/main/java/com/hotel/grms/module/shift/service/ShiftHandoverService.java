package com.hotel.grms.module.shift.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.billing.entity.Payment;
import com.hotel.grms.module.billing.mapper.PaymentMapper;
import com.hotel.grms.module.hk.HkTaskStatus;
import com.hotel.grms.module.hk.entity.HkTask;
import com.hotel.grms.module.hk.mapper.HkTaskMapper;
import com.hotel.grms.module.reservation.ReservationStatus;
import com.hotel.grms.module.reservation.entity.Reservation;
import com.hotel.grms.module.reservation.mapper.ReservationMapper;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.shift.ShiftSessionStatus;
import com.hotel.grms.module.shift.dto.HandoverPendingItem;
import com.hotel.grms.module.shift.dto.ShiftCloseRequest;
import com.hotel.grms.module.shift.dto.ShiftHandoverPreviewResponse;
import com.hotel.grms.module.shift.dto.ShiftHandoverResponse;
import com.hotel.grms.module.shift.entity.ShiftHandover;
import com.hotel.grms.module.shift.entity.ShiftSession;
import com.hotel.grms.module.shift.mapper.ShiftHandoverMapper;
import com.hotel.grms.module.shift.mapper.ShiftSessionMapper;
import com.hotel.grms.module.stay.StayStatus;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结班服务：收款汇总、待办交接、结班快照。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Service
public class ShiftHandoverService {

    private static final String METHOD_CASH = "CASH";
    private static final String METHOD_WECHAT = "WECHAT";
    private static final String METHOD_ALIPAY = "ALIPAY";

    private final ShiftSessionMapper shiftSessionMapper;
    private final ShiftHandoverMapper shiftHandoverMapper;
    private final PaymentMapper paymentMapper;
    private final StayOrderMapper stayOrderMapper;
    private final HkTaskMapper hkTaskMapper;
    private final ReservationMapper reservationMapper;
    private final RoomMapper roomMapper;
    private final ObjectMapper objectMapper;

    public ShiftHandoverService(ShiftSessionMapper shiftSessionMapper, ShiftHandoverMapper shiftHandoverMapper,
                                PaymentMapper paymentMapper, StayOrderMapper stayOrderMapper,
                                HkTaskMapper hkTaskMapper, ReservationMapper reservationMapper,
                                RoomMapper roomMapper, ObjectMapper objectMapper) {
        this.shiftSessionMapper = shiftSessionMapper;
        this.shiftHandoverMapper = shiftHandoverMapper;
        this.paymentMapper = paymentMapper;
        this.stayOrderMapper = stayOrderMapper;
        this.hkTaskMapper = hkTaskMapper;
        this.reservationMapper = reservationMapper;
        this.roomMapper = roomMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 结班预览：本班收款按支付方式汇总 + 全店待办。
     *
     * @param shiftSessionId 开班 ID
     * @return 预览数据
     */
    public ShiftHandoverPreviewResponse buildPreview(Long shiftSessionId) {
        ShiftSession session = requireOpenSessionOwned(shiftSessionId);
        ShiftHandoverPreviewResponse preview = new ShiftHandoverPreviewResponse();
        preview.setShiftSessionId(session.getId());
        preview.setOpenedAt(session.getOpenedAt());
        applyPaymentTotals(preview, session.getId());
        List<HandoverPendingItem> pending = collectPendingItems();
        preview.setPendingItems(pending);
        preview.setPendingCount(pending.size());
        preview.setBlockCloseOnPending(!pending.isEmpty());
        return preview;
    }

    /**
     * 结班：写入快照并关闭开班记录。
     *
     * @param shiftSessionId 开班 ID
     * @param request        结班请求
     * @return 结班单
     */
    @Transactional(rollbackFor = Exception.class)
    public ShiftHandoverResponse closeShift(Long shiftSessionId, ShiftCloseRequest request) {
        ShiftSession session = requireOpenSessionOwned(shiftSessionId);
        ShiftHandoverPreviewResponse preview = buildPreview(shiftSessionId);
        boolean forceClose = request != null && Boolean.TRUE.equals(request.getForceClose());
        if (preview.getPendingCount() > 0 && preview.isBlockCloseOnPending() && !forceClose) {
            throw new BusinessException(40033, "存在待办事项，请处理后再结班，或使用强制结班");
        }
        if (preview.getPendingCount() > 0 && forceClose && !SecurityUtils.hasAuthority("shift:force_close")) {
            throw new BusinessException(40005, "无强制结班权限");
        }
        ShiftHandover existing = shiftHandoverMapper.selectOne(new LambdaQueryWrapper<ShiftHandover>()
                .eq(ShiftHandover::getShiftSessionId, shiftSessionId));
        if (existing != null) {
            throw new BusinessException(40901, "该班次已结班");
        }
        ShiftHandover handover = new ShiftHandover();
        handover.setShiftSessionId(shiftSessionId);
        handover.setCashTotal(preview.getCashTotal());
        handover.setWechatTotal(preview.getWechatTotal());
        handover.setAlipayTotal(preview.getAlipayTotal());
        handover.setPendingSnapshot(writePendingJson(preview.getPendingItems()));
        handover.setCreatedAt(LocalDateTime.now());
        shiftHandoverMapper.insert(handover);
        session.setStatus(ShiftSessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        shiftSessionMapper.updateById(session);
        return toHandoverResponse(handover, session, preview.getPendingItems());
    }

    /**
     * 查询结班单详情。
     *
     * @param handoverId 结班单 ID
     * @return 详情
     */
    public ShiftHandoverResponse getHandover(Long handoverId) {
        ShiftHandover handover = shiftHandoverMapper.selectById(handoverId);
        if (handover == null) {
            throw new BusinessException(40034, "结班单不存在");
        }
        ShiftSession session = shiftSessionMapper.selectById(handover.getShiftSessionId());
        if (session == null) {
            throw new BusinessException(40034, "开班记录不存在");
        }
        return toHandoverResponse(handover, session, readPendingJson(handover.getPendingSnapshot()));
    }

    private ShiftSession requireOpenSessionOwned(Long shiftSessionId) {
        Long operatorId = requireOperatorId();
        ShiftSession session = shiftSessionMapper.selectById(shiftSessionId);
        if (session == null) {
            throw new BusinessException(40034, "开班记录不存在");
        }
        if (!operatorId.equals(session.getOperatorId())) {
            throw new BusinessException(40005, "无权操作他人班次");
        }
        if (!ShiftSessionStatus.OPEN.equals(session.getStatus())) {
            throw new BusinessException(40001, "班次已结班，无法操作");
        }
        return session;
    }

    private void applyPaymentTotals(ShiftHandoverPreviewResponse preview, Long shiftSessionId) {
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getShiftSessionId, shiftSessionId));
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal wechat = BigDecimal.ZERO;
        BigDecimal alipay = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.getAmount() == null) {
                continue;
            }
            String method = payment.getMethod();
            if (METHOD_CASH.equals(method)) {
                cash = cash.add(payment.getAmount());
            } else if (METHOD_WECHAT.equals(method)) {
                wechat = wechat.add(payment.getAmount());
            } else if (METHOD_ALIPAY.equals(method)) {
                alipay = alipay.add(payment.getAmount());
            }
        }
        preview.setCashTotal(cash);
        preview.setWechatTotal(wechat);
        preview.setAlipayTotal(alipay);
    }

    private List<HandoverPendingItem> collectPendingItems() {
        List<HandoverPendingItem> items = new ArrayList<HandoverPendingItem>();
        appendInHousePending(items);
        appendHkPending(items);
        appendReservationPending(items);
        return items;
    }

    private void appendInHousePending(List<HandoverPendingItem> items) {
        List<StayOrder> stays = stayOrderMapper.selectList(new LambdaQueryWrapper<StayOrder>()
                .eq(StayOrder::getStatus, StayStatus.IN_HOUSE)
                .orderByAsc(StayOrder::getRoomId));
        if (stays.isEmpty()) {
            return;
        }
        Map<Long, Room> roomMap = loadRoomMap(collectStayRoomIds(stays));
        for (StayOrder stay : stays) {
            HandoverPendingItem item = new HandoverPendingItem();
            item.setType("IN_HOUSE");
            item.setRefId(stay.getId());
            item.setTitle("未退房");
            Room room = roomMap.get(stay.getRoomId());
            String roomNo = room != null ? room.getRoomNo() : String.valueOf(stay.getRoomId());
            item.setDetail(roomNo + " " + stay.getGuestName() + " " + stay.getStayNo());
            items.add(item);
        }
    }

    private void appendHkPending(List<HandoverPendingItem> items) {
        List<HkTask> tasks = hkTaskMapper.selectList(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getStatus, HkTaskStatus.PENDING)
                .orderByAsc(HkTask::getRoomId));
        if (tasks.isEmpty()) {
            return;
        }
        Map<Long, Room> roomMap = loadRoomMap(collectHkRoomIds(tasks));
        for (HkTask task : tasks) {
            HandoverPendingItem item = new HandoverPendingItem();
            item.setType("HK");
            item.setRefId(task.getId());
            item.setTitle("待打扫");
            Room room = roomMap.get(task.getRoomId());
            String roomNo = room != null ? room.getRoomNo() : String.valueOf(task.getRoomId());
            item.setDetail("房号 " + roomNo);
            items.add(item);
        }
    }

    private void appendReservationPending(List<HandoverPendingItem> items) {
        List<Reservation> list = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
                .orderByAsc(Reservation::getArrivalDate));
        if (list.isEmpty()) {
            return;
        }
        Map<Long, Room> roomMap = loadRoomMap(collectResRoomIds(list));
        for (Reservation res : list) {
            HandoverPendingItem item = new HandoverPendingItem();
            item.setType("RESERVATION");
            item.setRefId(res.getId());
            item.setTitle("未释放预订");
            String roomPart = "";
            if (res.getRoomId() != null) {
                Room room = roomMap.get(res.getRoomId());
                roomPart = room != null ? "房号 " + room.getRoomNo() + " " : "";
            }
            item.setDetail(res.getResNo() + " " + roomPart + res.getGuestName());
            items.add(item);
        }
    }

    private List<Long> collectStayRoomIds(List<StayOrder> stays) {
        List<Long> ids = new ArrayList<Long>();
        for (StayOrder stay : stays) {
            if (stay.getRoomId() != null && !ids.contains(stay.getRoomId())) {
                ids.add(stay.getRoomId());
            }
        }
        return ids;
    }

    private List<Long> collectHkRoomIds(List<HkTask> tasks) {
        List<Long> ids = new ArrayList<Long>();
        for (HkTask task : tasks) {
            if (task.getRoomId() != null && !ids.contains(task.getRoomId())) {
                ids.add(task.getRoomId());
            }
        }
        return ids;
    }

    private List<Long> collectResRoomIds(List<Reservation> list) {
        List<Long> ids = new ArrayList<Long>();
        for (Reservation res : list) {
            if (res.getRoomId() != null && !ids.contains(res.getRoomId())) {
                ids.add(res.getRoomId());
            }
        }
        return ids;
    }

    private Map<Long, Room> loadRoomMap(List<Long> roomIds) {
        Map<Long, Room> map = new HashMap<Long, Room>();
        if (roomIds.isEmpty()) {
            return map;
        }
        List<Room> rooms = roomMapper.selectBatchIds(roomIds);
        for (Room room : rooms) {
            map.put(room.getId(), room);
        }
        return map;
    }

    private String writePendingJson(List<HandoverPendingItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception ex) {
            throw new BusinessException(50000, "待办快照序列化失败");
        }
    }

    private List<HandoverPendingItem> readPendingJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<HandoverPendingItem>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HandoverPendingItem>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<HandoverPendingItem>();
        }
    }

    private ShiftHandoverResponse toHandoverResponse(ShiftHandover handover, ShiftSession session,
                                                     List<HandoverPendingItem> pending) {
        ShiftHandoverResponse response = new ShiftHandoverResponse();
        response.setHandoverId(handover.getId());
        response.setShiftSessionId(session.getId());
        response.setOpenedAt(session.getOpenedAt());
        response.setClosedAt(session.getClosedAt());
        response.setCashTotal(handover.getCashTotal());
        response.setWechatTotal(handover.getWechatTotal());
        response.setAlipayTotal(handover.getAlipayTotal());
        response.setPendingItems(pending);
        return response;
    }

    private Long requireOperatorId() {
        Long operatorId = SecurityUtils.currentUserId();
        if (operatorId == null) {
            throw new BusinessException(40101, "未登录");
        }
        return operatorId;
    }
}
