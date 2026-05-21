package com.hotel.grms.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.auth.entity.SysUser;
import com.hotel.grms.module.auth.mapper.SysUserMapper;
import com.hotel.grms.module.auth.service.PermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 按用户名加载用户与权限的 UserDetailsService 实现。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class GrmsUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final PermissionService permissionService;

    public GrmsUserDetailsService(SysUserMapper sysUserMapper, PermissionService permissionService) {
        this.sysUserMapper = sysUserMapper;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    /**
     * 按用户 ID 加载登录用户。
     *
     * @param userId 用户 ID
     * @return 登录用户
     */
    public LoginUser loadUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40101, "用户不存在");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        List<String> codes = permissionService.listPermissionCodes(user.getId());
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(codes.size());
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), enabled, authorities);
    }
}
