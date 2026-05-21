package com.hotel.grms.module.hk.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 保洁任务接口集成测试（TC-07/TC-08）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HousekeepingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private int roomSeq = 1;
    /** 跨有序用例共享（JUnit 5 每方法新建测试实例） */
    private static Long taskId;
    private static Long roomId;

    @BeforeEach
    void login() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    @Order(1)
    void listPendingAfterCheckout() throws Exception {
        jdbcTemplate.update("DELETE FROM hk_task");
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM folio_line");
        jdbcTemplate.update("DELETE FROM folio");
        jdbcTemplate.update("DELETE FROM stay_guest");
        jdbcTemplate.update("DELETE FROM stay_order");
        jdbcTemplate.update("DELETE FROM shift_session");
        openShift();
        Long typeId = createRoomType();
        roomId = createRoom(typeId, "HK" + roomSeq++);
        Long stayId = walkInAndCheckout(roomId);
        mockMvc.perform(get("/api/v1/hk/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].roomNo").value("HK1"));
        taskId = jdbcTemplate.queryForObject(
                "SELECT id FROM hk_task WHERE room_id = ? AND status = 'PENDING'",
                Long.class, roomId);
        org.junit.jupiter.api.Assertions.assertNotNull(taskId);
        org.junit.jupiter.api.Assertions.assertNotNull(stayId);
    }

    @Test
    @Order(2)
    void completeTaskSetsVacantClean() throws Exception {
        mockMvc.perform(post("/api/v1/hk/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        String cleanStatus = jdbcTemplate.queryForObject(
                "SELECT clean_status FROM room WHERE id = ?",
                String.class, roomId);
        String occupancy = jdbcTemplate.queryForObject(
                "SELECT status FROM room WHERE id = ?",
                String.class, roomId);
        org.junit.jupiter.api.Assertions.assertEquals("CLEAN", cleanStatus);
        org.junit.jupiter.api.Assertions.assertEquals("VACANT", occupancy);
    }

    @Test
    @Order(3)
    void completeAgainReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/hk/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901));
    }

    private void openShift() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long walkInAndCheckout(Long roomId) throws Exception {
        WalkInCheckInRequest checkIn = new WalkInCheckInRequest();
        checkIn.setRoomId(roomId);
        checkIn.setGuestName("HK Guest");
        checkIn.setGuestPhone("13800001111");
        checkIn.setArrivalDate(LocalDate.now());
        checkIn.setDepartureDate(LocalDate.now().plusDays(2));
        checkIn.setAgreedDailyRate(new BigDecimal("100"));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(new BigDecimal("200"));
        payments.add(item);
        checkIn.setPayments(payments);
        MvcResult stayResult = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(stayResult.getResponse().getContentAsString()).path("data");
        Long stayId = data.path("id").asLong();
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        return stayId;
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest type = new RoomTypeRequest();
        type.setName("标准间HK" + roomSeq);
        type.setRackRate(new BigDecimal("200"));
        type.setBedType("双床");
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
        room.setFloorNo(3);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
