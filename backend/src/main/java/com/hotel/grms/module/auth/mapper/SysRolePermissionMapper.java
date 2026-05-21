package com.hotel.grms.module.auth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色权限关联数据访问接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Mapper
public interface SysRolePermissionMapper {

    /**
     * 删除角色全部权限关联。
     *
     * @param roleId 角色 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 插入角色权限关联。
     *
     * @param roleId       角色 ID
     * @param permissionId 权限 ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
