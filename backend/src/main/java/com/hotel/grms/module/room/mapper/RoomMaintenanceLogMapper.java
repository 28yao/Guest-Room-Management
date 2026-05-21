package com.hotel.grms.module.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.room.entity.RoomMaintenanceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 客房维修记录 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface RoomMaintenanceLogMapper extends BaseMapper<RoomMaintenanceLog> {

    /**
     * 查询客房未结束的维修记录。
     *
     * @param roomId 客房 ID
     * @return 进行中的维修记录，无则 null
     */
    @Select("SELECT * FROM room_maintenance_log WHERE room_id = #{roomId} AND ended_at IS NULL ORDER BY id DESC LIMIT 1")
    RoomMaintenanceLog selectOpenByRoomId(@Param("roomId") Long roomId);
}
