package com.hotel.grms.module.billing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.billing.dto.AddPaymentRequest;
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
 * 账单与退房接口集成测试。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private Long stayId;
    private Long folioId;
    private int roomSeq;

    @BeforeEach
    void login() throws Exception {
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM folio_line");
        jdbcTemplate.update("DELETE FROM folio");
        jdbcTemplate.update("DELETE FROM stay_guest");
        jdbcTemplate.update("DELETE FROM stay_order");
        jdbcTemplate.update("DELETE FROM hk_task");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM room_type");
        jdbcTemplate.update("DELETE FROM shift_session");
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
        roomSeq++;
    }

    @Test
    @Order(1)
    void checkInRequiresFullPayment() throws Exception {
        openShift();
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "BL" + roomSeq + "01");
        WalkInCheckInRequest checkIn = buildWalkIn(roomId);
        checkIn.setPayments(paymentList(new BigDecimal("100")));
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    @Order(2)
    void checkoutReleaseRoomAfterCheckInSettled() throws Exception {
        openShift();
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "BL" + roomSeq + "02");
        WalkInCheckInRequest checkIn = buildWalkIn(roomId);
        checkIn.setPayments(paymentList(new BigDecimal("300")));
        MvcResult stayResult = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(stayResult.getResponse().getContentAsString()).path("data");
        stayId = data.path("id").asLong();
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CHECKED_OUT"));
        Long hkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hk_task WHERE room_id = ?",
                Long.class, roomId);
        org.junit.jupiter.api.Assertions.assertTrue(hkCount != null && hkCount > 0);
    }

    @Test
    @Order(3)
    void adjustPriceSuccessForAdmin() throws Exception {
        openShift();
        prepareStayWithPayment();
        String body = "{\"agreedDailyRate\":280}";
        mockMvc.perform(post("/api/v1/folios/" + folioId + "/adjust-price")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void prepareStayWithPayment() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "BL" + roomSeq + "03");
        WalkInCheckInRequest checkIn = buildWalkIn(roomId);
        checkIn.setPayments(paymentList(new BigDecimal("300")));
        MvcResult stayResult = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(stayResult.getResponse().getContentAsString()).path("data");
        stayId = data.path("id").asLong();
        folioId = data.path("folioId").asLong();
    }

    private List<CheckInPaymentItem> paymentList(BigDecimal amount) {
        List<CheckInPaymentItem> list = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(amount);
        list.add(item);
        return list;
    }

    private void openShift() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private WalkInCheckInRequest buildWalkIn(Long roomId) {
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName("结账测试");
        request.setGuestPhone("13800009901");
        request.setArrivalDate(LocalDate.now());
        request.setDepartureDate(LocalDate.now().plusDays(1));
        request.setAgreedDailyRate(new BigDecimal("300"));
        request.setPayments(paymentList(new BigDecimal("300")));
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
        request.setFloorNo(9);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
