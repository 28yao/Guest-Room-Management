package com.hotel.grms.module.reservation.service;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.hotel.grms.common.BusinessException;

import com.hotel.grms.module.reservation.ReservationStatus;

import com.hotel.grms.module.reservation.dto.AvailableRoomDto;

import com.hotel.grms.module.reservation.entity.Reservation;

import com.hotel.grms.module.reservation.mapper.ReservationMapper;

import com.hotel.grms.module.reservation.support.ReservationTimePolicy;

import com.hotel.grms.module.stay.StayStatus;

import com.hotel.grms.module.stay.entity.StayOrder;

import com.hotel.grms.module.stay.mapper.StayOrderMapper;

import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;

import com.hotel.grms.module.room.entity.Room;

import com.hotel.grms.module.room.entity.RoomType;

import com.hotel.grms.module.room.mapper.RoomMapper;

import com.hotel.grms.module.room.service.RoomTypeService;

import org.springframework.stereotype.Service;



import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;



/**

 * 客房可售与可排房校验服务，防止超售（BR-01）与打扫缓冲冲突。

 *

 * @author liuxinsi

 * @date 2026-05-21

 */

@Service

public class RoomAvailabilityService {



    private final RoomMapper roomMapper;

    private final RoomTypeService roomTypeService;

    private final ReservationMapper reservationMapper;

    private final StayOrderMapper stayOrderMapper;



    public RoomAvailabilityService(RoomMapper roomMapper, RoomTypeService roomTypeService,

                                   ReservationMapper reservationMapper,

                                   StayOrderMapper stayOrderMapper) {

        this.roomMapper = roomMapper;

        this.roomTypeService = roomTypeService;

        this.reservationMapper = reservationMapper;

        this.stayOrderMapper = stayOrderMapper;

    }



    /**

     * 校验指定客房在时刻区间内可预排房。

     *

     * @param roomId               客房 ID

     * @param arrivalAt            入住时刻

     * @param departureAt          离店时刻

     * @param excludeReservationId 排除的预订 ID

     */

    /**
     * 校验客房在时刻区间内无在住单冲突（可排除指定在住单）。
     *
     * @param roomId         客房 ID
     * @param arrivalAt      入住时刻
     * @param departureAt    离店时刻
     * @param excludeStayId  排除的在住单 ID
     */
    public void assertNoStayTimeConflict(Long roomId, LocalDateTime arrivalAt, LocalDateTime departureAt,
                                       Long excludeStayId) {
        if (hasStayConflict(roomId, arrivalAt, departureAt, excludeStayId)) {
            throw new BusinessException(40002, "该客房在所选时段已有在住订单");
        }
    }

    public void assertAssignable(Long roomId, LocalDateTime arrivalAt, LocalDateTime departureAt,

                               Long excludeReservationId) {

        ReservationTimePolicy.assertValidRange(arrivalAt, departureAt);

        Room room = roomMapper.selectById(roomId);

        if (room == null) {

            throw new BusinessException(40013, "客房不存在");

        }

        if (hasReservationConflict(roomId, arrivalAt, departureAt, excludeReservationId)) {

            throw new BusinessException(40002, "该客房在所选时段已被占用或距上一单不足1小时打扫时间");

        }

        if (hasStayConflict(roomId, arrivalAt, departureAt, null)) {

            throw new BusinessException(40002, "该客房在所选时段已被在住单占用");

        }

        String occupancy = RoomStatus.normalizeOccupancy(room.getStatus());
        if (RoomStatus.OUT_OF_ORDER.equals(occupancy)) {
            throw new BusinessException(40001, "客房维修中不可预排房");
        }

    }



    /**

     * 查询可预排房客房列表。

     *

     * @param roomTypeId           房型 ID，可为 null

     * @param arrivalAt            入住时刻

     * @param departureAt          离店时刻

     * @param excludeReservationId 排除的预订 ID

     * @return 可排房列表

     */

    public List<AvailableRoomDto> listAssignableRooms(Long roomTypeId, LocalDateTime arrivalAt,

                                                      LocalDateTime departureAt, Long excludeReservationId) {

        ReservationTimePolicy.assertValidRange(arrivalAt, departureAt);

        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>()
                .in(Room::getStatus, RoomStatus.VACANT, RoomStatus.RESERVED)
                .eq(Room::getCleanStatus, RoomCleanStatus.CLEAN)
                .orderByAsc(Room::getFloorNo)

                .orderByAsc(Room::getRoomNo);

        if (roomTypeId != null) {

            wrapper.eq(Room::getRoomTypeId, roomTypeId);

        }

        List<Room> rooms = roomMapper.selectList(wrapper);

        Map<Long, RoomType> types = loadTypes(rooms);

        List<AvailableRoomDto> result = new ArrayList<AvailableRoomDto>();

        for (Room room : rooms) {

            if (!hasReservationConflict(room.getId(), arrivalAt, departureAt, excludeReservationId)

                    && !hasStayConflict(room.getId(), arrivalAt, departureAt, null)) {

                RoomType type = types.get(room.getRoomTypeId());

                AvailableRoomDto dto = new AvailableRoomDto();

                dto.setRoomId(room.getId());

                dto.setRoomNo(room.getRoomNo());

                dto.setRoomTypeId(room.getRoomTypeId());

                if (type != null) {

                    dto.setRoomTypeName(type.getName());

                    dto.setRackRate(type.getRackRate());

                }

                dto.setFloorNo(room.getFloorNo());

                dto.setVersion(room.getVersion());

                result.add(dto);

            }

        }

        prependPreAssignedRoom(result, types, roomTypeId, excludeReservationId, arrivalAt, departureAt);

        return result;

    }



    /**
     * 将当前预订已预排（RESERVED）的客房加入可选列表，供预订入住默认选中。
     */
    private void prependPreAssignedRoom(List<AvailableRoomDto> result, Map<Long, RoomType> types, Long roomTypeId,
                                        Long excludeReservationId, LocalDateTime arrivalAt,
                                        LocalDateTime departureAt) {
        if (excludeReservationId == null) {
            return;
        }
        Reservation reservation = reservationMapper.selectById(excludeReservationId);
        if (reservation == null || reservation.getRoomId() == null) {
            return;
        }
        Long assignedRoomId = reservation.getRoomId();
        for (AvailableRoomDto existing : result) {
            if (assignedRoomId.equals(existing.getRoomId())) {
                return;
            }
        }
        Room room = roomMapper.selectById(assignedRoomId);
        if (room == null) {
            return;
        }
        if (roomTypeId != null && !roomTypeId.equals(room.getRoomTypeId())) {
            return;
        }
        if (!RoomStatus.RESERVED.equals(room.getStatus())) {
            return;
        }
        if (hasStayConflict(room.getId(), arrivalAt, departureAt, null)) {
            return;
        }
        RoomType type = types.get(room.getRoomTypeId());
        if (type == null) {
            type = roomTypeService.getById(room.getRoomTypeId());
        }
        AvailableRoomDto dto = new AvailableRoomDto();
        dto.setRoomId(room.getId());
        dto.setRoomNo(room.getRoomNo());
        dto.setRoomTypeId(room.getRoomTypeId());
        if (type != null) {
            dto.setRoomTypeName(type.getName());
            dto.setRackRate(type.getRackRate());
        }
        dto.setFloorNo(room.getFloorNo());
        dto.setVersion(room.getVersion());
        result.add(0, dto);
    }



    private boolean hasReservationConflict(Long roomId, LocalDateTime arrivalAt, LocalDateTime departureAt,

                                           Long excludeReservationId) {

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()

                .eq(Reservation::getRoomId, roomId)

                .in(Reservation::getStatus, ReservationStatus.CONFIRMED, ReservationStatus.PENDING);

        if (excludeReservationId != null) {

            wrapper.ne(Reservation::getId, excludeReservationId);

        }

        List<Reservation> existing = reservationMapper.selectList(wrapper);

        for (Reservation reservation : existing) {

            if (ReservationTimePolicy.intervalsConflict(reservation.getArrivalAt(), reservation.getDepartureAt(),

                    arrivalAt, departureAt)) {

                return true;

            }

        }

        return false;

    }



    private boolean hasStayConflict(Long roomId, LocalDateTime arrivalAt, LocalDateTime departureAt,
                                    Long excludeStayId) {
        LambdaQueryWrapper<StayOrder> wrapper = new LambdaQueryWrapper<StayOrder>()
                .eq(StayOrder::getRoomId, roomId)
                .eq(StayOrder::getStatus, StayStatus.IN_HOUSE);
        if (excludeStayId != null) {
            wrapper.ne(StayOrder::getId, excludeStayId);
        }
        List<StayOrder> stays = stayOrderMapper.selectList(wrapper);
        for (StayOrder stay : stays) {
            LocalDateTime stayStart = ReservationTimePolicy.effectiveStayStart(stay);
            LocalDateTime stayEnd = ReservationTimePolicy.effectiveStayEnd(stay);
            if (ReservationTimePolicy.intervalsConflict(stayStart, stayEnd, arrivalAt, departureAt)) {
                return true;
            }
        }
        return false;
    }



    private Map<Long, RoomType> loadTypes(List<Room> rooms) {

        Map<Long, RoomType> map = new HashMap<Long, RoomType>();

        for (Room room : rooms) {

            Long typeId = room.getRoomTypeId();

            if (typeId != null && !map.containsKey(typeId)) {

                map.put(typeId, roomTypeService.getById(typeId));

            }

        }

        return map;

    }

}

