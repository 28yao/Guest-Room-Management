package com.hotel.grms.module.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.ReleaseReservationRequest;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 预订管理接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

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
    void assignRoomSetsReservedStatus() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "901");
        Long reservationId = createReservation(typeId);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roomId").value(roomId.intValue()));
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("RESERVED"));
    }

    @Test
    void overlappingAssignRejected() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "902");
        Long res1 = createReservation(typeId);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + res1 + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        Long res2 = createReservation(typeId);
        mockMvc.perform(post("/api/v1/reservations/" + res2 + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void releaseRestoresVacantClean() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "913");
        Long reservationId = createReservation(typeId);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        ReleaseReservationRequest release = new ReleaseReservationRequest();
        release.setNoShow(false);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/release")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(release)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("VACANT"));
    }

    @Test
    void cleaningBufferRejectsAssignWithinOneHour() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "905");
        LocalDate arrival = LocalDate.of(2026, 6, 1);
        LocalDate departure = LocalDate.of(2026, 6, 2);
        Long res1 = createReservation(typeId, arrival, departure);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + res1 + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        ReservationCreateRequest res2Req = new ReservationCreateRequest();
        res2Req.setGuestName("客人2");
        res2Req.setGuestPhone("13800002222");
        res2Req.setRoomTypeId(typeId);
        res2Req.setArrivalDate(departure);
        res2Req.setDepartureDate(departure.plusDays(1));
        res2Req.setArrivalAt(LocalDateTime.of(2026, 6, 2, 12, 30));
        res2Req.setDepartureAt(LocalDateTime.of(2026, 6, 3, 12, 0));
        MvcResult res2Result = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(res2Req)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long res2 = objectMapper.readTree(res2Result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/reservations/" + res2 + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void assignToMaintenanceRoomRejected() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "904");
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/maintenance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"测试\",\"expectedRecoveryAt\":\"2026-12-31T12:00:00\"}"))
                .andExpect(jsonPath("$.code").value(0));
        Long reservationId = createReservation(typeId);
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(40001));
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("预订测试房型");
        request.setRackRate(new BigDecimal("299"));
        MvcResult result = mockMvc.perform(post("/api/v1/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createRoom(Long typeId, String roomNo) throws Exception {
        RoomRequest request = new RoomRequest();
        request.setRoomNo(roomNo);
        request.setRoomTypeId(typeId);
        request.setFloorNo(9);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createReservation(Long typeId) throws Exception {
        LocalDate arrival = LocalDate.now().plusDays(1);
        return createReservation(typeId, arrival, arrival.plusDays(2));
    }

    private Long createReservation(Long typeId, LocalDate arrival, LocalDate departure) throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setGuestName("测试客人");
        request.setGuestPhone("13800001111");
        request.setRoomTypeId(typeId);
        request.setArrivalDate(arrival);
        request.setDepartureDate(departure);
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }
}
