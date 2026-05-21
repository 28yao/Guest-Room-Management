package com.hotel.grms.module.auth.dto;

import java.util.List;

/**
 * 更新用户请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class UserUpdateRequest {

    private String password;
    private Integer status;
    private List<Long> roleIds;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
