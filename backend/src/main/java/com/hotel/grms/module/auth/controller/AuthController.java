package com.hotel.grms.module.auth.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.auth.dto.LoginResponse;
import com.hotel.grms.module.auth.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、登出、当前用户。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return JWT 与权限列表
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 登出（客户端清除 token，服务端无状态）。
     *
     * @return 成功
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 用户与权限
     */
    @GetMapping("/me")
    public R<LoginResponse> me() {
        return R.ok(authService.currentUser());
    }
}
