package com.hotel.grms.module.stat.mapper;

import com.hotel.grms.module.stat.mapper.row.RevenueDailyRow;
import com.hotel.grms.module.stat.mapper.row.RevenueSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 经营统计聚合查询 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Mapper
public interface StatsMapper {

    /**
     * 客房总数。
     *
     * @return 间数
     */
    @Select("SELECT COUNT(*) FROM room")
    Long countTotalRooms();

    /**
     * 可售客房数（不含维修 OUT_OF_ORDER）。
     *
     * @return 间数
     */
    @Select("SELECT COUNT(*) FROM room WHERE status <> 'OUT_OF_ORDER'")
    Long countSellableRooms();

    /**
     * 当前在住间数。
     *
     * @return 间数
     */
    @Select("SELECT COUNT(*) FROM stay_order WHERE status = 'IN_HOUSE'")
    Long countInHouseRooms();

    /**
     * 区间内支付流水汇总（含退款负额）。
     *
     * @param start        区间起点（含）
     * @param endExclusive 区间终点（不含）
     * @return 汇总行
     */
    @Select("SELECT COALESCE(SUM(amount), 0) AS total_amount, "
            + "COALESCE(SUM(CASE WHEN method = 'CASH' THEN amount ELSE 0 END), 0) AS cash_total, "
            + "COALESCE(SUM(CASE WHEN method = 'WECHAT' THEN amount ELSE 0 END), 0) AS wechat_total, "
            + "COALESCE(SUM(CASE WHEN method = 'ALIPAY' THEN amount ELSE 0 END), 0) AS alipay_total "
            + "FROM payment WHERE paid_at >= #{start} AND paid_at < #{endExclusive}")
    RevenueSummaryRow sumRevenueInRange(@Param("start") LocalDateTime start,
                                        @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 按日汇总区间内支付流水。
     *
     * @param start        区间起点（含）
     * @param endExclusive 区间终点（不含）
     * @return 每日一行
     */
    @Select("SELECT DATE(paid_at) AS stat_date, COALESCE(SUM(amount), 0) AS total_amount "
            + "FROM payment WHERE paid_at >= #{start} AND paid_at < #{endExclusive} "
            + "GROUP BY DATE(paid_at) ORDER BY stat_date ASC")
    List<RevenueDailyRow> sumRevenueByDay(@Param("start") LocalDateTime start,
                                          @Param("endExclusive") LocalDateTime endExclusive);
}
