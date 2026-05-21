package com.hotel.grms.module.room.service;

import com.hotel.grms.module.room.DailyTag;
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
 * 房态图查询服务，组装指定日期叠加标签与展示房态。
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
        RoomBoardItemDto item = new RoomBoardItemDto();
        item.setId(row.getRoomId());
        item.setRoomNo(row.getRoomNo());
        item.setRoomTypeId(row.getRoomTypeId());
        item.setRoomTypeName(row.getRoomTypeName());
        item.setFloorNo(row.getFloorNo());
        item.setStatus(resolveDisplayStatus(row));
        item.setActualStatus(row.getStatus());
        item.setVersion(row.getVersion());
        item.setRackRate(row.getRackRate());
        item.setDailyTags(buildTags(row));
        return item;
    }

    private String resolveDisplayStatus(RoomBoardRowDto row) {
        String dbStatus = row.getStatus();
        boolean reservedOnView = row.getReservedOnViewDate() != null && row.getReservedOnViewDate() == 1;
        boolean inHouseOnView = row.getInHouseOnViewDate() != null && row.getInHouseOnViewDate() == 1;
        if (inHouseOnView) {
            return RoomStatus.OCCUPIED;
        }
        if (reservedOnView) {
            return RoomStatus.RESERVED;
        }
        if (RoomStatus.RESERVED.equals(dbStatus)) {
            return RoomStatus.VACANT_CLEAN;
        }
        if (RoomStatus.OCCUPIED.equals(dbStatus)) {
            return RoomStatus.VACANT_CLEAN;
        }
        return dbStatus;
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
