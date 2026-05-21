package com.hotel.grms.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
