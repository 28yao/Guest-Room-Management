package com.hotel.grms.module.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import com.hotel.grms.support.GrmsTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BillingService} 按晚计费边界测试（T-QA-SVC-01）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingServiceNightCountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        GrmsTestDataCleaner.cleanTransactionalData(jdbcTemplate);
        LoginRequest login = new LoginRequest();
        login.setUsername("admin");
        login.setPassword("admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void oneNightStayChargesSingleNight() throws Exception {
        Long stayId = walkIn(LocalDate.now(), LocalDate.now().plusDays(1), new BigDecimal("100"));
        assertActiveLineCount(stayId, 1);
        assertFolioTotal(stayId, new BigDecimal("100"));
    }

    @Test
    void threeNightStayChargesThreeNights() throws Exception {
        Long stayId = walkIn(LocalDate.now(), LocalDate.now().plusDays(3), new BigDecimal("120"));
        assertActiveLineCount(stayId, 3);
        assertFolioTotal(stayId, new BigDecimal("360"));
    }

    @Test
    void departureNotAfterArrivalRejected() throws Exception {
        LocalDate day = LocalDate.now();
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId);
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName("日期边界");
        request.setGuestPhone("13800003333");
        request.setArrivalDate(day);
        request.setDepartureDate(day);
        request.setAgreedDailyRate(new BigDecimal("88"));
        request.setPayments(singlePayment("CASH", new BigDecimal("88")));
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(40020));
    }

    private List<CheckInPaymentItem> singlePayment(String method, BigDecimal amount) {
        List<CheckInPaymentItem> list = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod(method);
        item.setAmount(amount);
        list.add(item);
        return list;
    }

    private Long walkIn(LocalDate arrival, LocalDate departure, BigDecimal dailyRate) throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId);
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName("晚数测试");
        request.setGuestPhone("13800004444");
        request.setArrivalDate(arrival);
        request.setDepartureDate(departure);
        request.setAgreedDailyRate(dailyRate);
        long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(arrival, departure));
        BigDecimal pay = dailyRate.multiply(BigDecimal.valueOf(nights));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(pay);
        payments.add(item);
        request.setPayments(payments);
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(stayRes.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void assertActiveLineCount(Long stayId, int expected) {
        Long folioId = jdbcTemplate.queryForObject(
                "SELECT id FROM folio WHERE stay_order_id = ?", Long.class, stayId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM folio_line WHERE folio_id = ? AND active = 1",
                Integer.class, folioId);
        assertEquals(expected, count.intValue());
    }

    private void assertFolioTotal(Long stayId, BigDecimal expected) throws Exception {
        mockMvc.perform(get("/api/v1/folios/by-stay/" + stayId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.totalAmount").value(expected.intValue()));
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest type = new RoomTypeRequest();
        type.setName("晚数房型");
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

    private Long createRoom(Long typeId) throws Exception {
        RoomRequest room = new RoomRequest();
        room.setRoomNo("NC" + System.nanoTime());
        room.setRoomTypeId(typeId);
        room.setFloorNo(6);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
