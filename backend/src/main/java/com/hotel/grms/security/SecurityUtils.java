package com.hotel.grms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具，获取当前登录用户标识。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return ((LoginUser) authentication.getPrincipal()).getUserId();
    }

    /**
     * 当前用户是否拥有指定权限码。
     *
     * @param authority 权限码
     * @return 是否拥有
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authority == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
