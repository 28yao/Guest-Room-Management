package com.hotel.grms.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.auth.dto.UserCreateRequest;
import com.hotel.grms.module.auth.dto.UserResponse;
import com.hotel.grms.module.auth.dto.UserUpdateRequest;
import com.hotel.grms.module.auth.entity.SysRole;
import com.hotel.grms.module.auth.entity.SysUser;
import com.hotel.grms.module.auth.mapper.SysRoleMapper;
import com.hotel.grms.module.auth.mapper.SysUserMapper;
import com.hotel.grms.module.auth.mapper.SysUserPermissionMapper;
import com.hotel.grms.module.auth.mapper.SysUserRoleMapper;
import com.hotel.grms.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统用户管理服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper,
                       SysUserRoleMapper sysUserRoleMapper, SysUserPermissionMapper sysUserPermissionMapper,
                       PermissionService permissionService, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysUserPermissionMapper = sysUserPermissionMapper;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询全部用户列表。
     *
     * @return 用户响应列表
     */
    public List<UserResponse> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        List<UserResponse> result = new ArrayList<UserResponse>(users.size());
        for (SysUser user : users) {
            result.add(toResponse(user));
        }
        return result;
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 用户响应
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserCreateRequest request) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(40010, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sysUserMapper.insert(user);
        saveUserRoles(user.getId(), request.getRoleIds());
        return toResponse(user);
    }

    /**
     * 更新用户。
     *
     * @param id      用户 ID
     * @param request 更新请求
     * @return 用户响应
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40011, "用户不存在");
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        sysUserMapper.updateById(user);
        if (request.getRoleIds() != null) {
            saveUserRoles(id, request.getRoleIds());
        }
        return toResponse(sysUserMapper.selectById(id));
    }

    /**
     * 删除用户及其角色、直授关联。
     *
     * @param id 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40011, "用户不存在");
        }
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException(40018, "内置管理员账号不可删除");
        }
        Long currentUserId = SecurityUtils.currentUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException(40019, "不能删除当前登录账号");
        }
        sysUserPermissionMapper.deleteByUserId(id);
        sysUserRoleMapper.deleteByUserId(id);
        sysUserMapper.deleteById(id);
    }

    /**
     * 管理员重置用户登录密码。
     *
     * @param id       用户 ID
     * @param password 新密码明文
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String password) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40011, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(password));
        sysUserMapper.updateById(user);
    }

    /**
     * 按用户名查询用户（登录用）。
     *
     * @param username 用户名
     * @return 用户实体
     */
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            sysUserRoleMapper.insert(userId, roleId);
        }
    }

    private UserResponse toResponse(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setStatus(user.getStatus());
        response.setRoleIds(sysUserRoleMapper.selectRoleIdsByUserId(user.getId()));
        response.setPermissions(permissionService.listPermissionCodes(user.getId()));
        List<Long> roleIds = response.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            List<String> roleNames = new ArrayList<String>(roles.size());
            for (SysRole role : roles) {
                roleNames.add(role.getName());
            }
            response.setRoleNames(roleNames);
        }
        return response;
    }
}
