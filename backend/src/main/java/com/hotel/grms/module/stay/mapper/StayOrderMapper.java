package com.hotel.grms.module.stay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.stay.entity.StayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 在住订单 Mapper，含在住列表聚合查询。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface StayOrderMapper extends BaseMapper<StayOrder> {

    /**
     * 查询在住列表展示行（单条 SQL，避免嵌套循环扫全表）。
     *
     * @return 在住行
     */
    @Select("SELECT s.id AS stay_id, s.stay_no, s.reservation_id, s.room_id, s.room_type_id, "
            + "s.arrival_date, s.departure_date, s.agreed_daily_rate, s.status, s.remark, s.check_in_at, "
            + "r.room_no, r.version AS room_version, rt.name AS room_type_name, "
            + "g.guest_name, g.guest_phone, g.id_card, "
            + "f.id AS folio_id, f.total_amount AS folio_total_amount, res.res_no "
            + "FROM stay_order s "
            + "INNER JOIN room r ON r.id = s.room_id "
            + "INNER JOIN room_type rt ON rt.id = s.room_type_id "
            + "LEFT JOIN stay_guest g ON g.stay_order_id = s.id "
            + "LEFT JOIN folio f ON f.stay_order_id = s.id "
            + "LEFT JOIN reservation res ON res.id = s.reservation_id "
            + "WHERE s.status = 'IN_HOUSE' "
            + "ORDER BY s.check_in_at DESC")
    List<StayInHouseRow> selectInHouseRows();

    /**
     * 统计指定客房是否已有在住单（排除指定在住单 ID）。
     *
     * @param roomId        客房 ID
     * @param excludeStayId 排除的在住单 ID
     * @return 条数
     */
    @Select("SELECT COUNT(*) FROM stay_order WHERE room_id = #{roomId} AND status = 'IN_HOUSE' "
            + "AND id <> #{excludeStayId}")
    int countInHouseOnRoom(@Param("roomId") Long roomId, @Param("excludeStayId") Long excludeStayId);
}
