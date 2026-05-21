package com.hotel.grms.module.stat.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.stat.dto.OccupancyStatsResponse;
import com.hotel.grms.module.stat.dto.RevenueStatsResponse;
import com.hotel.grms.module.stat.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 经营统计 REST 接口。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 出租率快照。
     *
     * @return 在住/可售/出租率
     */
    @GetMapping("/occupancy")
    @PreAuthorize("hasAuthority('stat:view')")
    public R<OccupancyStatsResponse> occupancy() {
        return R.ok(statsService.getOccupancySnapshot());
    }

    /**
     * 区间房费营收。
     *
     * @param from 起始日
     * @param to   结束日
     * @return 营收汇总
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('stat:view')")
    public R<RevenueStatsResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(statsService.getRevenue(from, to));
    }
}
