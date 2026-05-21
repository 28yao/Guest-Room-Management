package com.hotel.grms.module.auth.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.auth.dto.PermissionAssignItemDto;
import com.hotel.grms.module.auth.dto.PermissionIdsRequest;
import com.hotel.grms.module.auth.dto.RoleResponse;
import com.hotel.grms.module.auth.entity.SysRole;
import com.hotel.grms.module.auth.mapper.SysRoleMapper;
import com.hotel.grms.module.auth.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色与角色权限配置接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final SysRoleMapper sysRoleMapper;
    private final PermissionService permissionService;

    public RoleController(SysRoleMapper sysRoleMapper, PermissionService permissionService) {
        this.sysRoleMapper = sysRoleMapper;
        this.permissionService = permissionService;
    }

    /**
     * 角色列表。
     *
     * @return 角色列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:manage')")
    public R<List<RoleResponse>> list() {
        List<SysRole> roles = sysRoleMapper.selectList(null);
        List<RoleResponse> result = new ArrayList<RoleResponse>(roles.size());
        for (SysRole role : roles) {
            RoleResponse item = new RoleResponse();
            item.setId(role.getId());
            item.setCode(role.getCode());
            item.setName(role.getName());
            item.setDescription(role.getDescription());
            result.add(item);
        }
        return R.ok(result);
    }

    /**
     * 查询角色权限分配项。
     *
     * @param id 角色 ID
     * @return 权限项列表
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public R<List<PermissionAssignItemDto>> listPermissions(@PathVariable Long id) {
        return R.ok(permissionService.listRolePermissionItems(id));
    }

    /**
     * 保存角色权限。
     *
     * @param id      角色 ID
     * @param request 权限 ID 列表
     * @return 成功
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public R<Void> savePermissions(@PathVariable Long id, @RequestBody PermissionIdsRequest request) {
        permissionService.saveRolePermissions(id, request.getPermissionIds());
        return R.ok();
    }

    /**
     * 将角色权限恢复为系统默认（与种子数据一致）。
     *
     * @param id 角色 ID
     * @return 恢复后的权限分配项
     */
    @PostMapping("/{id}/permissions/restore-default")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public R<List<PermissionAssignItemDto>> restoreDefaultPermissions(@PathVariable Long id) {
        return R.ok(permissionService.restoreRolePermissionsToDefault(id));
    }
}
