package com.hotel.grms.module.stat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 经营统计接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String frontToken;

    @BeforeEach
    void login() throws Exception {
        adminToken = loginAs("admin");
        frontToken = loginAs("front");
    }

    @Test
    @Order(1)
    void occupancyReturnsCounts() throws Exception {
        mockMvc.perform(get("/api/v1/stats/occupancy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sellableRooms").isNumber())
                .andExpect(jsonPath("$.data.inHouseRooms").isNumber())
                .andExpect(jsonPath("$.data.occupancyRate").isNumber());
    }

    @Test
    @Order(2)
    void revenueMatchesPaymentInRange() throws Exception {
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM folio_line");
        jdbcTemplate.update("DELETE FROM folio");
        jdbcTemplate.update("DELETE FROM stay_guest");
        jdbcTemplate.update("DELETE FROM stay_order");
        jdbcTemplate.update("DELETE FROM shift_handover");
        jdbcTemplate.update("DELETE FROM shift_session");

        mockMvc.perform(post("/api/v1/shifts/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));

        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "ST901");
        WalkInCheckInRequest checkIn = new WalkInCheckInRequest();
        checkIn.setRoomId(roomId);
        checkIn.setGuestName("统计测试");
        checkIn.setGuestPhone("13800008888");
        checkIn.setArrivalDate(LocalDate.now());
        checkIn.setDepartureDate(LocalDate.now().plusDays(1));
        checkIn.setAgreedDailyRate(new BigDecimal("300"));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(new BigDecimal("300"));
        payments.add(item);
        checkIn.setPayments(payments);
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0));

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/stats/revenue")
                        .param("from", today.toString())
                        .param("to", today.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalRevenue").value(300))
                .andExpect(jsonPath("$.data.cashTotal").value(300));
    }

    @Test
    @Order(3)
    void revenueRejectsInvalidRange() throws Exception {
        mockMvc.perform(get("/api/v1/stats/revenue")
                        .param("from", "2026-05-10")
                        .param("to", "2026-05-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(40025));
    }

    @Test
    @Order(4)
    void frontDeskForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/stats/occupancy")
                        .header("Authorization", "Bearer " + frontToken))
                .andExpect(status().isForbidden());
    }

    private String loginAs(String username) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest type = new RoomTypeRequest();
        type.setName("统计房型");
        type.setRackRate(new BigDecimal("300"));
        type.setBedType("大床");
        type.setWindowType("有窗");
        type.setNonSmoking(1);
        type.setMaxGuests(2);
        MvcResult result = mockMvc.perform(post("/api/v1/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(type)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createRoom(Long typeId, String roomNo) throws Exception {
        RoomRequest room = new RoomRequest();
        room.setRoomNo(roomNo);
        room.setRoomTypeId(typeId);
        room.setFloorNo(9);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
