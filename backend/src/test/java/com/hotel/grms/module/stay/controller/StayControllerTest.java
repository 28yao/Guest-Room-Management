package com.hotel.grms.module.stay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.stay.dto.CheckInFromReservationRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import com.hotel.grms.support.GrmsTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
 * 入住接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void login() throws Exception {
        GrmsTestDataCleaner.cleanTransactionalData(jdbcTemplate);
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
    void walkInRequiresOpenShift() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "ST9" + System.nanoTime());
        WalkInCheckInRequest checkIn = buildWalkIn(roomId);
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003));
    }

    @Test
    @Order(2)
    void walkInSuccessAfterOpenShift() throws Exception {
        openShift();
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "ST9" + System.nanoTime());
        WalkInCheckInRequest checkIn = buildWalkIn(roomId);
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_HOUSE"));
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("OCCUPIED"));
        mockMvc.perform(get("/api/v1/stays/in-house").param("guestName", "WalkIn-" + roomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(3)
    void checkInFromReservationSuccess() throws Exception {
        openShift();
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "ST9" + System.nanoTime());
        Long reservationId = createReservation(typeId);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        CheckInFromReservationRequest checkIn = new CheckInFromReservationRequest();
        checkIn.setReservationId(reservationId);
        checkIn.setRoomId(roomId);
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem pay = new CheckInPaymentItem();
        pay.setMethod("CASH");
        pay.setAmount(new BigDecimal("388"));
        payments.add(pay);
        checkIn.setPayments(payments);
        mockMvc.perform(post("/api/v1/stays/check-in-from-reservation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reservationId").value(reservationId.intValue()));
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("OCCUPIED"));
    }

    private void openShift() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private WalkInCheckInRequest buildWalkIn(Long roomId) {
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName("WalkIn-" + roomId);
        request.setGuestPhone("13800000001");
        request.setArrivalDate(LocalDate.now());
        request.setDepartureDate(LocalDate.now().plusDays(1));
        request.setAgreedDailyRate(new BigDecimal("300"));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem pay = new CheckInPaymentItem();
        pay.setMethod("CASH");
        pay.setAmount(new BigDecimal("300"));
        payments.add(pay);
        request.setPayments(payments);
        return request;
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("标准间");
        request.setRackRate(new BigDecimal("388"));
        request.setBedType("双床");
        request.setWindowType("有窗");
        request.setNonSmoking(1);
        request.setMaxGuests(2);
        MvcResult result = mockMvc.perform(post("/api/v1/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createRoom(Long typeId, String roomNo) throws Exception {
        RoomRequest request = new RoomRequest();
        request.setRoomNo(roomNo);
        request.setRoomTypeId(typeId);
        request.setFloorNo(8);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createReservation(Long typeId) throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setGuestName("李四");
        request.setGuestPhone("13900000002");
        request.setRoomTypeId(typeId);
        request.setArrivalDate(LocalDate.now());
        request.setDepartureDate(LocalDate.now().plusDays(1));
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
