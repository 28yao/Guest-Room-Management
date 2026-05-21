package com.hotel.grms.module.shift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.shift.dto.ShiftCloseRequest;
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
 * 交班结班接口集成测试（TC-10）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private static Long shiftId;
    private static Long stayId;
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
    void openShiftAndPreviewWithPayment() throws Exception {
        jdbcTemplate.update("DELETE FROM shift_handover");
        jdbcTemplate.update("DELETE FROM hk_task");
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM folio_line");
        jdbcTemplate.update("DELETE FROM folio");
        jdbcTemplate.update("DELETE FROM stay_guest");
        jdbcTemplate.update("DELETE FROM stay_order");
        jdbcTemplate.update("DELETE FROM shift_session");

        MvcResult openRes = mockMvc.perform(post("/api/v1/shifts/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        shiftId = objectMapper.readTree(openRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        Long typeId = createRoomType();
        roomId = createRoom(typeId, "SH901");
        WalkInCheckInRequest checkIn = new WalkInCheckInRequest();
        checkIn.setRoomId(roomId);
        checkIn.setGuestName("交班测试");
        checkIn.setGuestPhone("13800009999");
        checkIn.setArrivalDate(LocalDate.now());
        checkIn.setDepartureDate(LocalDate.now().plusDays(1));
        checkIn.setAgreedDailyRate(new BigDecimal("200"));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(new BigDecimal("200"));
        payments.add(item);
        checkIn.setPayments(payments);
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        stayId = objectMapper.readTree(stayRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/shifts/" + shiftId + "/handover-preview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cashTotal").value(200));
    }

    @Test
    @Order(2)
    void closeShiftSuccess() throws Exception {
        clearPendingBeforeClose();
        ShiftCloseRequest body = new ShiftCloseRequest();
        body.setForceClose(false);
        mockMvc.perform(post("/api/v1/shifts/" + shiftId + "/close")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cashTotal").value(200));
    }

    @Test
    @Order(3)
    void closeAgainWhenAlreadyClosed() throws Exception {
        ShiftCloseRequest body = new ShiftCloseRequest();
        mockMvc.perform(post("/api/v1/shifts/" + shiftId + "/close")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(40001));
    }

    private void clearPendingBeforeClose() throws Exception {
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        Long hkTaskId = jdbcTemplate.queryForObject(
                "SELECT id FROM hk_task WHERE room_id = ? AND status = 'PENDING'",
                Long.class, roomId);
        mockMvc.perform(post("/api/v1/hk/tasks/" + hkTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest type = new RoomTypeRequest();
        type.setName("交班房型");
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
