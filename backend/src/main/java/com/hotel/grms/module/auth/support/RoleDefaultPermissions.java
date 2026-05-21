package com.hotel.grms.module.auth.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 预置角色默认权限码，与 sql/V2__seed_data.sql 种子数据一致。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class RoleDefaultPermissions {

    /** 管理员：全部权限（由服务层解析为所有权限点 ID） */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final Set<String> MANAGER_CODES = codes(
            "room:manage", "room:status:maintenance", "room:status:dirty", "room:status:clean",
            "room:status:force",
            "stat:view", "audit:view", "shift:force_close", "reservation:manage",
            "stay:checkin", "stay:change_room", "billing:checkout", "hk:view");

    private static final Set<String> FRONT_DESK_CODES = codes(
            "reservation:manage", "stay:checkin", "stay:change_room",
            "billing:checkout", "shift:open", "shift:close",
            "room:status:maintenance", "room:status:dirty", "hk:view");

    private static final Set<String> HOUSEKEEPING_CODES = codes("hk:view", "hk:complete", "room:status:clean");

    private RoleDefaultPermissions() {
    }

    /**
     * 是否管理员角色（默认拥有全部权限点）。
     *
     * @param roleCode 角色编码
     * @return 是否管理员
     */
    public static boolean isAdminRole(String roleCode) {
        return ROLE_ADMIN.equals(roleCode);
    }

    /**
     * 获取角色默认权限码集合；管理员返回 null 表示全部权限。
     *
     * @param roleCode 角色编码
     * @return 权限码集合，未知角色返回空集合
     */
    public static Set<String> permissionCodesFor(String roleCode) {
        if (roleCode == null) {
            return Collections.emptySet();
        }
        if (ROLE_ADMIN.equals(roleCode)) {
            return null;
        }
        if ("ROLE_MANAGER".equals(roleCode)) {
            return MANAGER_CODES;
        }
        if ("ROLE_FRONT_DESK".equals(roleCode)) {
            return FRONT_DESK_CODES;
        }
        if ("ROLE_HOUSEKEEPING".equals(roleCode)) {
            return HOUSEKEEPING_CODES;
        }
        return Collections.emptySet();
    }

    private static Set<String> codes(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}
