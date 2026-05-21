package com.hotel.grms.module.room.service;

import com.hotel.grms.module.room.DailyTag;
import com.hotel.grms.module.room.dto.RoomBoardItemDto;
import com.hotel.grms.module.room.dto.RoomBoardRowDto;
import com.hotel.grms.module.room.mapper.RoomBoardMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 房态图查询服务，组装当日叠加标签。
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
     * @param floorNo 楼层筛选
     * @return 房态图项列表
     */
    public List<RoomBoardItemDto> loadBoard(Integer floorNo) {
        List<RoomBoardRowDto> rows = roomBoardMapper.selectBoardRows(floorNo);
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
        item.setStatus(row.getStatus());
        item.setVersion(row.getVersion());
        item.setRackRate(row.getRackRate());
        item.setDailyTags(buildTags(row));
        return item;
    }

    private List<String> buildTags(RoomBoardRowDto row) {
        List<String> tags = new ArrayList<String>(2);
        if (row.getExpectedArrival() != null && row.getExpectedArrival() == 1) {
            tags.add(DailyTag.EXPECTED_ARRIVAL);
        }
        if (row.getExpectedDeparture() != null && row.getExpectedDeparture() == 1) {
            tags.add(DailyTag.EXPECTED_DEPARTURE);
        }
        return tags;
    }
}
