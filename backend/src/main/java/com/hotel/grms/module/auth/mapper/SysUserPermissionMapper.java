package com.hotel.grms.module.auth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户直授权限关联数据访问接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface SysUserPermissionMapper {

    /**
     * 删除用户全部直授权限。
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_user_permission WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 插入用户直授权限。
     *
     * @param userId       用户 ID
     * @param permissionId 权限 ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_user_permission (user_id, permission_id) VALUES (#{userId}, #{permissionId})")
    int insert(@Param("userId") Long userId, @Param("permissionId") Long permissionId);
}
