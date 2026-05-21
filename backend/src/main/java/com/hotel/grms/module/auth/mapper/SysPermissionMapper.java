package com.hotel.grms.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.grms.module.auth.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限点数据访问接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户通过角色获得的权限码。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    @Select("SELECT DISTINCT p.code FROM sys_permission p "
            + "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id "
            + "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id "
            + "WHERE ur.user_id = #{userId}")
    List<String> selectCodesByRole(@Param("userId") Long userId);

    /**
     * 查询用户直授权限码。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    @Select("SELECT p.code FROM sys_permission p "
            + "INNER JOIN sys_user_permission up ON p.id = up.permission_id "
            + "WHERE up.user_id = #{userId}")
    List<String> selectCodesByDirectGrant(@Param("userId") Long userId);

    /**
     * 查询角色已分配的权限 ID。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询用户直授权限 ID。
     *
     * @param userId 用户 ID
     * @return 权限 ID 列表
     */
    @Select("SELECT permission_id FROM sys_user_permission WHERE user_id = #{userId}")
    List<Long> selectPermissionIdsByUserId(@Param("userId") Long userId);
}
