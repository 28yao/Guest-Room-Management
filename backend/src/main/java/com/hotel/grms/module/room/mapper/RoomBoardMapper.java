package com.hotel.grms.module.room.mapper;

import com.hotel.grms.module.room.dto.RoomBoardRowDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房态图聚合查询 Mapper，单次 SQL 拉取客房与指定日期叠加标签。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface RoomBoardMapper {

    /**
     * 查询房态图行数据。
     *
     * @param floorNo        楼层筛选，null 表示全部
     * @param viewDate       查看日期
     * @param viewDateStart  查看日 00:00:00
     * @param viewDateEnd    查看日次日 00:00:00
     * @return 房态图行列表
     */
    @Select("<script>"
            + "SELECT r.id AS room_id, r.room_no, r.room_type_id, rt.name AS room_type_name, "
            + "r.floor_no, r.status, r.version, rt.rack_rate, "
            + "CASE WHEN EXISTS (SELECT 1 FROM reservation res WHERE res.room_id = r.id "
            + "AND res.status IN ('CONFIRMED','PENDING') "
            + "AND CAST(res.arrival_at AS DATE) = #{viewDate}) "
            + "THEN 1 ELSE 0 END AS expected_arrival, "
            + "CASE WHEN EXISTS (SELECT 1 FROM stay_order so WHERE so.room_id = r.id "
            + "AND so.departure_date = #{viewDate} AND so.status = 'IN_HOUSE') "
            + "THEN 1 ELSE 0 END AS expected_departure, "
            + "CASE WHEN EXISTS (SELECT 1 FROM reservation res WHERE res.room_id = r.id "
            + "AND res.status IN ('CONFIRMED','PENDING') "
            + "AND res.arrival_at &lt; #{viewDateEnd} AND res.departure_at &gt; #{viewDateStart}) "
            + "THEN 1 ELSE 0 END AS reserved_on_view_date, "
            + "CASE WHEN EXISTS (SELECT 1 FROM reservation res WHERE res.room_id = r.id "
            + "AND res.status IN ('CONFIRMED','PENDING') "
            + "AND CAST(res.departure_at AS DATE) = #{viewDate}) "
            + "THEN 1 ELSE 0 END AS expected_res_departure "
            + "FROM room r INNER JOIN room_type rt ON rt.id = r.room_type_id "
            + "<where>"
            + "<if test='floorNo != null'> AND r.floor_no = #{floorNo} </if>"
            + "</where>"
            + "ORDER BY r.floor_no ASC, r.room_no ASC"
            + "</script>")
    List<RoomBoardRowDto> selectBoardRows(@Param("floorNo") Integer floorNo,
                                          @Param("viewDate") LocalDate viewDate,
                                          @Param("viewDateStart") LocalDateTime viewDateStart,
                                          @Param("viewDateEnd") LocalDateTime viewDateEnd);
}
