package com.hotel.grms.module.auth.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.auth.dto.PermissionAssignItemDto;
import com.hotel.grms.module.auth.dto.PermissionIdsRequest;
import com.hotel.grms.module.auth.dto.UserCreateRequest;
import com.hotel.grms.module.auth.dto.UserPasswordRequest;
import com.hotel.grms.module.auth.dto.UserResponse;
import com.hotel.grms.module.auth.dto.UserUpdateRequest;
import com.hotel.grms.module.auth.service.PermissionService;
import com.hotel.grms.module.auth.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统用户管理接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;

    public UserController(UserService userService, PermissionService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    /**
     * 用户列表。
     *
     * @return 用户列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:manage')")
    public R<List<UserResponse>> list() {
        return R.ok(userService.listUsers());
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 新用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:manage')")
    public R<UserResponse> create(@Validated @RequestBody UserCreateRequest request) {
        return R.ok(userService.createUser(request));
    }

    /**
     * 更新用户。
     *
     * @param id      用户 ID
     * @param request 更新请求
     * @return 更新后用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public R<UserResponse> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return R.ok(userService.updateUser(id, request));
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return 成功
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }

    /**
     * 查询用户直授权限项。
     *
     * @param id 用户 ID
     * @return 权限分配项
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:permission:grant')")
    public R<List<PermissionAssignItemDto>> listDirectPermissions(@PathVariable Long id) {
        return R.ok(permissionService.listUserDirectPermissionItems(id));
    }

    /**
     * 保存用户直授权限。
     *
     * @param id      用户 ID
     * @param request 权限 ID 列表
     * @return 成功
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:permission:grant')")
    public R<Void> saveDirectPermissions(@PathVariable Long id, @RequestBody PermissionIdsRequest request) {
        permissionService.saveUserDirectPermissions(id, request.getPermissionIds());
        return R.ok();
    }

    /**
     * 清空用户直授权限，恢复为默认（无直授）。
     *
     * @param id 用户 ID
     * @return 恢复后的权限分配项
     */
    @PostMapping("/{id}/permissions/restore-default")
    @PreAuthorize("hasAuthority('system:permission:grant')")
    public R<List<PermissionAssignItemDto>> restoreDefaultDirectPermissions(@PathVariable Long id) {
        return R.ok(permissionService.restoreUserDirectPermissionsToDefault(id));
    }

    /**
     * 修改用户登录密码。
     *
     * @param id      用户 ID
     * @param request 新密码
     * @return 成功
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public R<Void> changePassword(@PathVariable Long id, @Validated @RequestBody UserPasswordRequest request) {
        userService.changePassword(id, request.getPassword());
        return R.ok();
    }
}
