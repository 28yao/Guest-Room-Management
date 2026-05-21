package com.hotel.grms.module.auth.service;

import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.auth.dto.LoginResponse;
import com.hotel.grms.module.auth.entity.SysUser;
import com.hotel.grms.security.JwtTokenProvider;
import com.hotel.grms.security.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务，处理登录与当前用户信息。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class AuthService {

    private final UserService userService;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserService userService, PermissionService permissionService,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户登录并签发 JWT。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(40100, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(40102, "账号已停用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(40100, "用户名或密码错误");
        }
        List<String> permissions = permissionService.listPermissionCodes(user.getId());
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), permissions);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setPermissions(permissions);
        return response;
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 登录响应（无 token）
     */
    public LoginResponse currentUser() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LoginResponse response = new LoginResponse();
        response.setUserId(loginUser.getUserId());
        response.setUsername(loginUser.getUsername());
        response.setPermissions(permissionService.listPermissionCodes(loginUser.getUserId()));
        return response;
    }
}
