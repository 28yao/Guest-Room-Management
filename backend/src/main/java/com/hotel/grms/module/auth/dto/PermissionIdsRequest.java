package com.hotel.grms.module.auth.dto;

import java.util.List;

/**
 * 权限 ID 列表请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class PermissionIdsRequest {

    private List<Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
