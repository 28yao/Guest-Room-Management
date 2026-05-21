package com.hotel.grms.module.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
