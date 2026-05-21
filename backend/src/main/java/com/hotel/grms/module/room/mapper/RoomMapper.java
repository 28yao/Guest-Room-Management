package com.hotel.grms.module.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.room.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客房表 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    /**
     * 查询全部楼层号（去重升序）。
     *
     * @return 楼层列表
     */
    @Select("SELECT DISTINCT floor_no FROM room ORDER BY floor_no ASC")
    List<Integer> selectDistinctFloors();
}
