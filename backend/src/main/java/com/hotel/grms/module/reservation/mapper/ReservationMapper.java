package com.hotel.grms.module.reservation.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.hotel.grms.module.reservation.entity.Reservation;

import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

import org.apache.ibatis.annotations.Select;



import java.time.LocalDate;



/**

 * 预订表 Mapper，含日期冲突统计查询。

 *

 * @author liuxinsi

 * @date 2026-05-21

 */

@Mapper

public interface ReservationMapper extends BaseMapper<Reservation> {



    /**

     * 统计同房间在住区间内的在住单冲突数（按日期，在住模块未改时刻前沿用）。

     *

     * @param roomId    客房 ID

     * @param arrival   入住日

     * @param departure 离店日

     * @return 冲突条数

     */

    @Select("SELECT COUNT(*) FROM stay_order WHERE room_id = #{roomId} AND status = 'IN_HOUSE' "

            + "AND arrival_date < #{departure} AND departure_date > #{arrival}")

    int countRoomStayConflict(@Param("roomId") Long roomId,

                              @Param("arrival") LocalDate arrival,

                              @Param("departure") LocalDate departure);

}

