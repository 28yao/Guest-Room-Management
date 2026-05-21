package com.hotel.grms.module.auth.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.auth.dto.PermissionAssignItemDto;
import com.hotel.grms.module.auth.entity.SysPermission;
import com.hotel.grms.module.auth.entity.SysRole;
import com.hotel.grms.module.auth.mapper.SysPermissionMapper;
import com.hotel.grms.module.auth.mapper.SysRoleMapper;
import com.hotel.grms.module.auth.mapper.SysRolePermissionMapper;
import com.hotel.grms.module.auth.mapper.SysUserPermissionMapper;
import com.hotel.grms.module.auth.support.RoleDefaultPermissions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限查询与分配服务，合并角色权限与用户直授。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class PermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;

    public PermissionService(SysPermissionMapper sysPermissionMapper,
                               SysRoleMapper sysRoleMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysUserPermissionMapper sysUserPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserPermissionMapper = sysUserPermissionMapper;
    }

    /**
     * 查询用户有效权限码（角色 + 直授去重）。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    public List<String> listPermissionCodes(Long userId) {
        Set<String> merged = new HashSet<String>();
        merged.addAll(sysPermissionMapper.selectCodesByRole(userId));
        merged.addAll(sysPermissionMapper.selectCodesByDirectGrant(userId));
        return new ArrayList<String>(merged);
    }

    /**
     * 查询全部权限及角色已选状态。
     *
     * @param roleId 角色 ID
     * @return 权限分配项列表
     */
    public List<PermissionAssignItemDto> listRolePermissionItems(Long roleId) {
        List<SysPermission> all = sysPermissionMapper.selectList(null);
        List<Long> assigned = sysPermissionMapper.selectPermissionIdsByRoleId(roleId);
        Set<Long> assignedSet = new HashSet<Long>(assigned);
        return buildAssignItems(all, assignedSet);
    }

    /**
     * 查询全部权限及用户直授已选状态。
     *
     * @param userId 用户 ID
     * @return 权限分配项列表
     */
    public List<PermissionAssignItemDto> listUserDirectPermissionItems(Long userId) {
        List<SysPermission> all = sysPermissionMapper.selectList(null);
        List<Long> assigned = sysPermissionMapper.selectPermissionIdsByUserId(userId);
        Set<Long> assignedSet = new HashSet<Long>(assigned);
        return buildAssignItems(all, assignedSet);
    }

    /**
     * 保存角色权限关联。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        if (permissionIds == null) {
            throw new BusinessException(40015, "权限列表不能为空");
        }
        sysRolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : permissionIds) {
            sysRolePermissionMapper.insert(roleId, permissionId);
        }
    }

    /**
     * 保存用户直授权限。
     *
     * @param userId        用户 ID
     * @param permissionIds 权限 ID 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveUserDirectPermissions(Long userId, List<Long> permissionIds) {
        if (permissionIds == null) {
            throw new BusinessException(40015, "权限列表不能为空");
        }
        sysUserPermissionMapper.deleteByUserId(userId);
        for (Long permissionId : permissionIds) {
            sysUserPermissionMapper.insert(userId, permissionId);
        }
    }

    /**
     * 将角色权限恢复为 MVP 种子数据中的默认配置。
     *
     * @param roleId 角色 ID
     * @return 恢复后的权限分配项
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionAssignItemDto> restoreRolePermissionsToDefault(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(40012, "角色不存在");
        }
        List<SysPermission> all = sysPermissionMapper.selectList(null);
        List<Long> defaultIds = resolveDefaultPermissionIds(role.getCode(), all);
        saveRolePermissions(roleId, defaultIds);
        return listRolePermissionItems(roleId);
    }

    /**
     * 清空用户直授权限（默认无直授，仅继承角色权限）。
     *
     * @param userId 用户 ID
     * @return 恢复后的权限分配项
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionAssignItemDto> restoreUserDirectPermissionsToDefault(Long userId) {
        saveUserDirectPermissions(userId, Collections.<Long>emptyList());
        return listUserDirectPermissionItems(userId);
    }

    private List<Long> resolveDefaultPermissionIds(String roleCode, List<SysPermission> all) {
        Set<String> codes = RoleDefaultPermissions.permissionCodesFor(roleCode);
        List<Long> ids = new ArrayList<Long>();
        for (SysPermission permission : all) {
            if (codes == null || codes.contains(permission.getCode())) {
                ids.add(permission.getId());
            }
        }
        return ids;
    }

    private List<PermissionAssignItemDto> buildAssignItems(List<SysPermission> all, Set<Long> assignedSet) {
        List<PermissionAssignItemDto> items = new ArrayList<PermissionAssignItemDto>(all.size());
        for (SysPermission permission : all) {
            PermissionAssignItemDto item = new PermissionAssignItemDto();
            item.setId(permission.getId());
            item.setCode(permission.getCode());
            item.setName(permission.getName());
            item.setDescription(permission.getDescription());
            item.setAssigned(assignedSet.contains(permission.getId()));
            items.add(item);
        }
        return items;
    }
}
