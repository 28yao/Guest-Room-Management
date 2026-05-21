package com.hotel.grms.module.stay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.stay.entity.StayGuest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 在住客人 Mapper。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface StayGuestMapper extends BaseMapper<StayGuest> {
}
