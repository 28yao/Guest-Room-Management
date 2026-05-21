package com.hotel.grms.module.hk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 保洁角色访问隔离：不可查看房态图与在住列表。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HousekeepingAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String hkToken;

    @BeforeEach
    void loginHk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("hk01");
        request.setPassword("admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        hkToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void hkCannotAccessRoomBoard() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/board")
                        .header("Authorization", "Bearer " + hkToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hkCannotAccessInHouseList() throws Exception {
        mockMvc.perform(get("/api/v1/stays/in-house")
                        .header("Authorization", "Bearer " + hkToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hkCanListHkTasks() throws Exception {
        mockMvc.perform(get("/api/v1/hk/tasks")
                        .header("Authorization", "Bearer " + hkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void hkCanListFloorsForHkFilter() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/floors")
                        .header("Authorization", "Bearer " + hkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
