package com.hotel.grms.module.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 管理员重置用户密码请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class UserPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6～64 位")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
