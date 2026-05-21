package com.hotel.grms.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.auth.dto.PermissionIdsRequest;
import com.hotel.grms.module.auth.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证与用户权限接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 登录成功返回 token。
     */
    @Test
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists());
    }

    /**
     * 密码错误返回业务错误码。
     */
    @Test
    void loginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }

    /**
     * 无 token 创建用户返回 401。
     */
    @Test
    void createUserWithoutTokenUnauthorized() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 前台账号无用户管理权限返回 403。
     */
    @Test
    void createUserWithoutPermissionForbidden() throws Exception {
        resetFrontDeskRolePermissions(Arrays.asList(3L));
        String token = loginAndGetToken("front", "admin123");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser2");
        request.setPassword("123456");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * 管理员可删除普通用户。
     */
    @Test
    void deleteUserSuccess() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("todel");
        request.setPassword("123456");
        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
        mockMvc.perform(delete("/api/v1/users/" + userId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 内置 admin 账号不可删除。
     */
    @Test
    void deleteAdminUserRejected() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        mockMvc.perform(delete("/api/v1/users/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40018));
    }

    /**
     * 不能删除当前登录账号。
     */
    @Test
    void deleteSelfUserRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("selfdel");
        request.setPassword("123456");
        request.setRoleIds(Collections.singletonList(1L));
        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
        String selfToken = loginAndGetToken("selfdel", "123456");
        mockMvc.perform(delete("/api/v1/users/" + userId).header("Authorization", "Bearer " + selfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40019));
    }

    /**
     * 为前台角色授予用户管理权限后，前台账号可访问用户列表。
     */
    @Test
    void frontUserWithGrantedRolePermissionCanListUsers() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        PermissionIdsRequest grant = new PermissionIdsRequest();
        grant.setPermissionIds(Arrays.asList(1L, 3L, 8L));
        mockMvc.perform(put("/api/v1/roles/2/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String frontToken = loginAndGetToken("front", "admin123");
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + frontToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 无效 token 访问 me 返回 401。
     */
    @Test
    void meWithInvalidTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    private void resetFrontDeskRolePermissions(java.util.List<Long> permissionIds) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        PermissionIdsRequest req = new PermissionIdsRequest();
        req.setPermissionIds(permissionIds);
        mockMvc.perform(put("/api/v1/roles/2/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }
}
