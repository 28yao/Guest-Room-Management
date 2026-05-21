package com.hotel.grms.module.stat.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.stat.dto.OccupancyStatsResponse;
import com.hotel.grms.module.stat.dto.RevenueDailyItem;
import com.hotel.grms.module.stat.dto.RevenueStatsResponse;
import com.hotel.grms.module.stat.mapper.StatsMapper;
import com.hotel.grms.module.stat.mapper.row.RevenueDailyRow;
import com.hotel.grms.module.stat.mapper.row.RevenueSummaryRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营统计服务：出租率快照与区间营收汇总。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Service
public class StatsService {

    private static final int MAX_RANGE_DAYS = 366;

    private final StatsMapper statsMapper;

    public StatsService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    /**
     * 当前出租率快照：在住间数 / 可售间数。
     *
     * @return 出租率数据
     */
    public OccupancyStatsResponse getOccupancySnapshot() {
        int total = toInt(statsMapper.countTotalRooms());
        int sellable = toInt(statsMapper.countSellableRooms());
        int inHouse = toInt(statsMapper.countInHouseRooms());
        OccupancyStatsResponse response = new OccupancyStatsResponse();
        response.setTotalRooms(total);
        response.setSellableRooms(sellable);
        response.setInHouseRooms(inHouse);
        response.setOccupancyRate(calcOccupancyRate(inHouse, sellable));
        return response;
    }

    /**
     * 区间房费营收（payment 流水净额，含退款负额）。
     *
     * @param fromDate 起始日（含）
     * @param toDate   结束日（含）
     * @return 营收汇总
     */
    public RevenueStatsResponse getRevenue(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        validateDateRange(from, to);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        RevenueSummaryRow summary = statsMapper.sumRevenueInRange(start, endExclusive);
        List<RevenueDailyRow> dailyRows = statsMapper.sumRevenueByDay(start, endExclusive);
        RevenueStatsResponse response = new RevenueStatsResponse();
        response.setFromDate(from);
        response.setToDate(to);
        response.setTotalRevenue(zeroIfNull(summary != null ? summary.getTotalAmount() : null));
        response.setCashTotal(zeroIfNull(summary != null ? summary.getCashTotal() : null));
        response.setWechatTotal(zeroIfNull(summary != null ? summary.getWechatTotal() : null));
        response.setAlipayTotal(zeroIfNull(summary != null ? summary.getAlipayTotal() : null));
        response.setDailyItems(buildDailySeries(from, to, dailyRows));
        return response;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(40025, "统计结束日期不能早于开始日期");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BusinessException(40025, "统计区间不能超过366天");
        }
    }

    private BigDecimal calcOccupancyRate(int inHouse, int sellable) {
        if (sellable <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(inHouse)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sellable), 2, RoundingMode.HALF_UP);
    }

    private List<RevenueDailyItem> buildDailySeries(LocalDate from, LocalDate to, List<RevenueDailyRow> rows) {
        Map<LocalDate, BigDecimal> amountByDate = new HashMap<LocalDate, BigDecimal>();
        if (rows != null) {
            for (RevenueDailyRow row : rows) {
                if (row.getStatDate() != null) {
                    amountByDate.put(row.getStatDate(), zeroIfNull(row.getTotalAmount()));
                }
            }
        }
        List<RevenueDailyItem> items = new ArrayList<RevenueDailyItem>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            RevenueDailyItem item = new RevenueDailyItem();
            item.setDate(cursor);
            BigDecimal amount = amountByNull(amountByDate.get(cursor));
            item.setAmount(amount);
            items.add(item);
            cursor = cursor.plusDays(1);
        }
        return items;
    }

    private BigDecimal amountByNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int toInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
}
