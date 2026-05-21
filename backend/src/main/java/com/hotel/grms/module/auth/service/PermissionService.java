package com.hotel.grms.module.auth.service;

import com.hotel.grms.module.auth.dto.PermissionAssignItemDto;
import com.hotel.grms.module.auth.entity.SysPermission;
import com.hotel.grms.module.auth.mapper.SysPermissionMapper;
import com.hotel.grms.module.auth.mapper.SysRolePermissionMapper;
import com.hotel.grms.module.auth.mapper.SysUserPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;

    public PermissionService(SysPermissionMapper sysPermissionMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysUserPermissionMapper sysUserPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
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
        sysRolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds == null) {
            return;
        }
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
        sysUserPermissionMapper.deleteByUserId(userId);
        if (permissionIds == null) {
            return;
        }
        for (Long permissionId : permissionIds) {
            sysUserPermissionMapper.insert(userId, permissionId);
        }
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
