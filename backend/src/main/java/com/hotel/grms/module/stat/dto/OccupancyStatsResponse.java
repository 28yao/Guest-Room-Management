package com.hotel.grms.module.stat.dto;

import java.math.BigDecimal;

/**
 * 出租率快照响应。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class OccupancyStatsResponse {

    private int totalRooms;
    private int sellableRooms;
    private int inHouseRooms;
    private BigDecimal occupancyRate;

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getSellableRooms() {
        return sellableRooms;
    }

    public void setSellableRooms(int sellableRooms) {
        this.sellableRooms = sellableRooms;
    }

    public int getInHouseRooms() {
        return inHouseRooms;
    }

    public void setInHouseRooms(int inHouseRooms) {
        this.inHouseRooms = inHouseRooms;
    }

    public BigDecimal getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(BigDecimal occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}
