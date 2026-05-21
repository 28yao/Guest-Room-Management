package com.hotel.grms.module.reservation.service;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.hotel.grms.common.BusinessException;

import com.hotel.grms.module.reservation.ReservationStatus;

import com.hotel.grms.module.reservation.dto.AvailableRoomDto;

import com.hotel.grms.module.reservation.entity.Reservation;

import com.hotel.grms.module.reservation.mapper.ReservationMapper;

import com.hotel.grms.module.reservation.support.ReservationTimePolicy;

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



    public RoomAvailabilityService(RoomMapper roomMapper, RoomTypeService roomTypeService,

                                   ReservationMapper reservationMapper) {

        this.roomMapper = roomMapper;

        this.roomTypeService = roomTypeService;

        this.reservationMapper = reservationMapper;

    }



    /**

     * 校验指定客房在时刻区间内可预排房。

     *

     * @param roomId               客房 ID

     * @param arrivalAt            入住时刻

     * @param departureAt          离店时刻

     * @param excludeReservationId 排除的预订 ID

     */

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

        if (hasStayConflict(roomId, arrivalAt.toLocalDate(), departureAt.toLocalDate())) {

            throw new BusinessException(40002, "该客房在所选日期已被在住单占用");

        }

        if (!RoomStatus.VACANT_CLEAN.equals(room.getStatus())) {

            throw new BusinessException(40001, "客房当前状态不可预排房");

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

                .eq(Room::getStatus, RoomStatus.VACANT_CLEAN)

                .orderByAsc(Room::getFloorNo)

                .orderByAsc(Room::getRoomNo);

        if (roomTypeId != null) {

            wrapper.eq(Room::getRoomTypeId, roomTypeId);

        }

        List<Room> rooms = roomMapper.selectList(wrapper);

        Map<Long, String> typeNames = loadTypeNames(rooms);

        List<AvailableRoomDto> result = new ArrayList<AvailableRoomDto>();

        for (Room room : rooms) {

            if (!hasReservationConflict(room.getId(), arrivalAt, departureAt, excludeReservationId)

                    && !hasStayConflict(room.getId(), arrivalAt.toLocalDate(), departureAt.toLocalDate())) {

                AvailableRoomDto dto = new AvailableRoomDto();

                dto.setRoomId(room.getId());

                dto.setRoomNo(room.getRoomNo());

                dto.setRoomTypeId(room.getRoomTypeId());

                dto.setRoomTypeName(typeNames.get(room.getRoomTypeId()));

                dto.setFloorNo(room.getFloorNo());

                dto.setVersion(room.getVersion());

                result.add(dto);

            }

        }

        return result;

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



    private boolean hasStayConflict(Long roomId, LocalDate arrival, LocalDate departure) {

        int stayCount = reservationMapper.countRoomStayConflict(roomId, arrival, departure);

        return stayCount > 0;

    }



    private Map<Long, String> loadTypeNames(List<Room> rooms) {

        Map<Long, String> map = new HashMap<Long, String>();

        for (Room room : rooms) {

            Long typeId = room.getRoomTypeId();

            if (typeId != null && !map.containsKey(typeId)) {

                RoomType type = roomTypeService.getById(typeId);

                map.put(typeId, type.getName());

            }

        }

        return map;

    }

}

