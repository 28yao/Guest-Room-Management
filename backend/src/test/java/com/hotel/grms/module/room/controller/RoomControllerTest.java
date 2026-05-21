package com.hotel.grms.module.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
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

import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 客房与房态接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerTest {

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
    void boardReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/board")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 指定查看日期查询房态图。
     */
    @Test
    void boardWithViewDateReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-12-25")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void boardHidesReservationOutsideViewDate() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "805");
        Long reservationId = createReservation(typeId, LocalDate.of(2026, 5, 22), LocalDate.of(2026, 5, 23));
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-24")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("VACANT_CLEAN"))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].actualStatus").value("RESERVED"));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-22")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("RESERVED"));
    }

    /**
     * 查看日超出在住离店日后，展示态不再为在住（库内仍为 OCCUPIED）。
     */
    @Test
    void boardHidesInHouseOutsideViewDate() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "808");
        WalkInCheckInRequest checkIn = new WalkInCheckInRequest();
        checkIn.setRoomId(roomId);
        checkIn.setGuestName("在住展示测试");
        checkIn.setGuestPhone("13800004444");
        checkIn.setArrivalDate(LocalDate.of(2026, 5, 22));
        checkIn.setDepartureDate(LocalDate.of(2026, 5, 23));
        checkIn.setAgreedDailyRate(new BigDecimal("300"));
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-23")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("OCCUPIED"));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-24")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("VACANT_CLEAN"))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].actualStatus").value("OCCUPIED"));
    }

    @Test
    void boardShowsReservationWhenRoomDirtyOnViewDate() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "809");
        Long reservationId = createReservation(typeId, LocalDate.of(2026, 5, 22), LocalDate.of(2026, 5, 23));
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/dirty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-22")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("RESERVED"))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].actualStatus").value("DIRTY"));
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/clean")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/rooms/board")
                        .param("date", "2026-05-22")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("RESERVED"));
    }

    @Test
    void listFloorsReturnsAll() throws Exception {
        Long typeId = createRoomType();
        createRoom(typeId, "806", 2);
        createRoom(typeId, "807", 5);
        mockMvc.perform(get("/api/v1/rooms/floors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void duplicateRoomNoRejected() throws Exception {
        Long typeId = createRoomType();
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setRoomNo("802");
        roomRequest.setRoomTypeId(typeId);
        roomRequest.setFloorNo(8);
        String body = objectMapper.writeValueAsString(roomRequest);
        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(40014));
    }

    @Test
    void markDirtyThenCleanSuccess() throws Exception {
        Long roomId = createRoom();
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/dirty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DIRTY"));
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/clean")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("VACANT_CLEAN"));
    }

    private Long createRoom() throws Exception {
        long typeId = createRoomType();
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setRoomNo("801");
        roomRequest.setRoomTypeId(typeId);
        roomRequest.setFloorNo(8);
        MvcResult roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createRoom(Long typeId, String roomNo) throws Exception {
        return createRoom(typeId, roomNo, 8);
    }

    private Long createRoom(Long typeId, String roomNo, int floorNo) throws Exception {
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setRoomNo(roomNo);
        roomRequest.setRoomTypeId(typeId);
        roomRequest.setFloorNo(floorNo);
        MvcResult roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createReservation(Long typeId, LocalDate arrival, LocalDate departure) throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setGuestName("房态测试");
        request.setGuestPhone("13800003333");
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

    private long createRoomType() throws Exception {
        RoomTypeRequest typeRequest = new RoomTypeRequest();
        typeRequest.setName("标准大床");
        typeRequest.setRackRate(new BigDecimal("299.00"));
        MvcResult typeResult = mockMvc.perform(post("/api/v1/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(typeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(typeResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }
}
