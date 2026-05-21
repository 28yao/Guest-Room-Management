package com.hotel.grms.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.reservation.ReservationStatus;
import com.hotel.grms.module.reservation.entity.Reservation;
import com.hotel.grms.module.reservation.mapper.ReservationMapper;
import com.hotel.grms.module.reservation.support.ReservationTimePolicy;
import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.dto.RoomScheduleDto;
import com.hotel.grms.module.room.dto.RoomScheduleOrderDto;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.mapper.RoomMapper;
import com.hotel.grms.module.stay.StayStatus;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 房态图客房日程服务：查询指定客房自某日起的预订/在住订单。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomScheduleService {

    private static final String ORDER_TYPE_RESERVATION = "RESERVATION";
    private static final String ORDER_TYPE_STAY = "STAY";

    private final RoomMapper roomMapper;
    private final RoomTypeService roomTypeService;
    private final ReservationMapper reservationMapper;
    private final StayOrderMapper stayOrderMapper;

    public RoomScheduleService(RoomMapper roomMapper, RoomTypeService roomTypeService,
                               ReservationMapper reservationMapper, StayOrderMapper stayOrderMapper) {
        this.roomMapper = roomMapper;
        this.roomTypeService = roomTypeService;
        this.reservationMapper = reservationMapper;
        this.stayOrderMapper = stayOrderMapper;
    }

    /**
     * 加载客房日程（当前及未来有效订单）。
     *
     * @param roomId   客房 ID
     * @param fromDate 查看日期，null 为当天
     * @return 日程数据
     */
    public RoomScheduleDto loadSchedule(Long roomId, LocalDate fromDate) {
        LocalDate viewDate = fromDate == null ? LocalDate.now() : fromDate;
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(40013, "客房不存在");
        }
        RoomType roomType = roomTypeService.getById(room.getRoomTypeId());
        List<RoomScheduleOrderDto> orders = loadOrders(roomId, viewDate);
        Collections.sort(orders, Comparator.comparing(RoomScheduleOrderDto::getArrivalAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        RoomScheduleDto dto = new RoomScheduleDto();
        dto.setRoomId(room.getId());
        dto.setRoomNo(room.getRoomNo());
        dto.setRoomTypeId(room.getRoomTypeId());
        dto.setRoomTypeName(roomType.getName());
        dto.setRackRate(roomType.getRackRate());
        dto.setOccupancyStatus(RoomStatus.normalizeOccupancy(room.getStatus()));
        dto.setCleanStatus(resolveCleanStatus(room));
        dto.setVersion(room.getVersion());
        dto.setViewDate(viewDate);
        dto.setOrders(orders);
        dto.setOccupiedOnViewDate(computeOccupiedOnViewDate(orders, viewDate));
        return dto;
    }

    private List<RoomScheduleOrderDto> loadOrders(Long roomId, LocalDate fromDate) {
        List<RoomScheduleOrderDto> result = new ArrayList<RoomScheduleOrderDto>();
        appendReservations(result, roomId, fromDate);
        appendStays(result, roomId, fromDate);
        return result;
    }

    private void appendReservations(List<RoomScheduleOrderDto> result, Long roomId, LocalDate fromDate) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getRoomId, roomId)
                .in(Reservation::getStatus, ReservationStatus.CONFIRMED, ReservationStatus.PENDING)
                .ge(Reservation::getDepartureDate, fromDate)
                .orderByAsc(Reservation::getArrivalAt);
        List<Reservation> list = reservationMapper.selectList(wrapper);
        for (Reservation entity : list) {
            RoomScheduleOrderDto item = new RoomScheduleOrderDto();
            item.setOrderType(ORDER_TYPE_RESERVATION);
            item.setOrderId(entity.getId());
            item.setOrderNo(entity.getResNo());
            item.setGuestName(entity.getGuestName());
            item.setGuestPhone(entity.getGuestPhone());
            item.setArrivalDate(entity.getArrivalDate());
            item.setDepartureDate(entity.getDepartureDate());
            item.setArrivalAt(entity.getArrivalAt());
            item.setDepartureAt(entity.getDepartureAt());
            item.setStatus(entity.getStatus());
            item.setRemark(entity.getRemark());
            item.setEditable(Boolean.TRUE);
            result.add(item);
        }
    }

    private void appendStays(List<RoomScheduleOrderDto> result, Long roomId, LocalDate fromDate) {
        LambdaQueryWrapper<StayOrder> wrapper = new LambdaQueryWrapper<StayOrder>()
                .eq(StayOrder::getRoomId, roomId)
                .eq(StayOrder::getStatus, StayStatus.IN_HOUSE)
                .ge(StayOrder::getDepartureDate, fromDate)
                .orderByAsc(StayOrder::getCheckInAt);
        List<StayOrder> list = stayOrderMapper.selectList(wrapper);
        for (StayOrder entity : list) {
            RoomScheduleOrderDto item = new RoomScheduleOrderDto();
            item.setOrderType(ORDER_TYPE_STAY);
            item.setOrderId(entity.getId());
            item.setOrderNo(entity.getStayNo());
            item.setGuestName(entity.getGuestName());
            item.setGuestPhone(entity.getGuestPhone());
            item.setArrivalDate(entity.getArrivalDate());
            item.setDepartureDate(entity.getDepartureDate());
            item.setArrivalAt(ReservationTimePolicy.effectiveStayStart(entity));
            item.setDepartureAt(ReservationTimePolicy.effectiveStayEnd(entity));
            item.setStatus(entity.getStatus());
            item.setRemark(entity.getRemark());
            item.setAgreedDailyRate(entity.getAgreedDailyRate());
            item.setEditable(Boolean.TRUE);
            result.add(item);
        }
    }

    private boolean computeOccupiedOnViewDate(List<RoomScheduleOrderDto> orders, LocalDate viewDate) {
        for (RoomScheduleOrderDto order : orders) {
            if (order.getArrivalAt() == null || order.getDepartureAt() == null) {
                continue;
            }
            if (ReservationTimePolicy.occupiesViewDate(viewDate, order.getArrivalAt(), order.getDepartureAt())) {
                return true;
            }
        }
        return false;
    }

    private String resolveCleanStatus(Room room) {
        if (room.getCleanStatus() != null && !room.getCleanStatus().isEmpty()) {
            return room.getCleanStatus();
        }
        if (RoomStatus.DIRTY.equals(room.getStatus())) {
            return RoomCleanStatus.DIRTY;
        }
        return RoomCleanStatus.CLEAN;
    }
}
