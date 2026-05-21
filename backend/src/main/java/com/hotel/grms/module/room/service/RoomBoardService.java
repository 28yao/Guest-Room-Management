package com.hotel.grms.module.room.service;

import com.hotel.grms.module.room.DailyTag;
import com.hotel.grms.module.room.RoomCleanStatus;
import com.hotel.grms.module.room.RoomStatus;
import com.hotel.grms.module.room.dto.RoomBoardItemDto;
import com.hotel.grms.module.room.dto.RoomBoardRowDto;
import com.hotel.grms.module.room.mapper.RoomBoardMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 房态图查询服务：占用态与保洁态分维展示。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomBoardService {

    private final RoomBoardMapper roomBoardMapper;

    public RoomBoardService(RoomBoardMapper roomBoardMapper) {
        this.roomBoardMapper = roomBoardMapper;
    }

    /**
     * 查询房态图数据。
     *
     * @param floorNo  楼层筛选
     * @param viewDate 查看日期，null 时使用当天
     * @return 房态图项列表
     */
    public List<RoomBoardItemDto> loadBoard(Integer floorNo, LocalDate viewDate) {
        LocalDate date = viewDate == null ? LocalDate.now() : viewDate;
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<RoomBoardRowDto> rows = roomBoardMapper.selectBoardRows(floorNo, date, dayStart, dayEnd);
        List<RoomBoardItemDto> items = new ArrayList<RoomBoardItemDto>(rows.size());
        for (RoomBoardRowDto row : rows) {
            items.add(toItem(row));
        }
        return items;
    }

    private RoomBoardItemDto toItem(RoomBoardRowDto row) {
        String occupancy = RoomStatus.normalizeOccupancy(row.getStatus());
        String clean = resolveCleanStatus(row);
        RoomBoardItemDto item = new RoomBoardItemDto();
        item.setId(row.getRoomId());
        item.setRoomNo(row.getRoomNo());
        item.setRoomTypeId(row.getRoomTypeId());
        item.setRoomTypeName(row.getRoomTypeName());
        item.setFloorNo(row.getFloorNo());
        item.setOccupancyStatus(occupancy);
        item.setCleanStatus(clean);
        item.setStatus(resolveDisplayOccupancy(row, occupancy));
        item.setVersion(row.getVersion());
        item.setRackRate(row.getRackRate());
        item.setDailyTags(buildTags(row));
        return item;
    }

    private String resolveCleanStatus(RoomBoardRowDto row) {
        if (row.getCleanStatus() != null && !row.getCleanStatus().isEmpty()) {
            return row.getCleanStatus();
        }
        if (RoomStatus.DIRTY.equals(row.getStatus())) {
            return RoomCleanStatus.DIRTY;
        }
        return RoomCleanStatus.CLEAN;
    }

    private String resolveDisplayOccupancy(RoomBoardRowDto row, String occupancy) {
        boolean reservedOnView = row.getReservedOnViewDate() != null && row.getReservedOnViewDate() == 1;
        boolean inHouseOnView = row.getInHouseOnViewDate() != null && row.getInHouseOnViewDate() == 1;
        if (inHouseOnView) {
            return RoomStatus.OCCUPIED;
        }
        if (reservedOnView) {
            return RoomStatus.RESERVED;
        }
        return occupancy;
    }

    private List<String> buildTags(RoomBoardRowDto row) {
        Set<String> tagSet = new LinkedHashSet<String>();
        if (row.getExpectedArrival() != null && row.getExpectedArrival() == 1) {
            tagSet.add(DailyTag.EXPECTED_ARRIVAL);
        }
        if (row.getExpectedDeparture() != null && row.getExpectedDeparture() == 1) {
            tagSet.add(DailyTag.EXPECTED_DEPARTURE);
        }
        if (row.getExpectedResDeparture() != null && row.getExpectedResDeparture() == 1) {
            tagSet.add(DailyTag.EXPECTED_DEPARTURE);
        }
        return new ArrayList<String>(tagSet);
    }
}
