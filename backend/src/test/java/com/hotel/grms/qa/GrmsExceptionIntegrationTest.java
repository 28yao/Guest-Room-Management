package com.hotel.grms.qa;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MOD-QA 异常验收用例（plan TC-11/TC-12）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GrmsExceptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String frontToken;

    @BeforeEach
    void setUp() throws Exception {
        GrmsTestDataCleaner.cleanTransactionalData(jdbcTemplate);
        adminToken = login("admin");
        frontToken = login("front");
    }

    /** TC-11：同一客房重复入住被拒绝。 */
    @Test
    void tc11_duplicateCheckInOnOccupiedRoomRejected() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "EX11");
        WalkInCheckInRequest first = buildWalkIn(roomId, "首住");
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(jsonPath("$.code").value(0));
        WalkInCheckInRequest second = buildWalkIn(roomId, "二住");
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(jsonPath("$.code").value(40001));
    }

    /** TC-12：强制改房态须权限+原因；前台 403，管理员成功并审计。 */
    @Test
    void tc12_forceStatusRequiresPermissionReasonAndAudit() throws Exception {
        Long typeId = createRoomType();
        Long roomId = createRoom(typeId, "EX12");
        String forceBody = "{\"targetStatus\":\"OCCUPIED\",\"reason\":\"验收强改\"}";
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/force")
                        .header("Authorization", "Bearer " + frontToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forceBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/status/force")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forceBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("OCCUPIED"));
        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("bizType", "ROOM")
                        .param("operationType", "ROOM_FORCE_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.records[0].bizId").value(roomId.intValue()))
                .andExpect(jsonPath("$.data.records[0].summary").value("强制改房态"));
    }

    private String login(String username) throws Exception {
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

    private WalkInCheckInRequest buildWalkIn(Long roomId, String guestName) {
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName(guestName);
        request.setGuestPhone("13800005555");
        request.setArrivalDate(LocalDate.now());
        request.setDepartureDate(LocalDate.now().plusDays(1));
        request.setAgreedDailyRate(new BigDecimal("100"));
        List<CheckInPaymentItem> payments = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod("CASH");
        item.setAmount(new BigDecimal("100"));
        payments.add(item);
        request.setPayments(payments);
        return request;
    }

    private Long createRoomType() throws Exception {
        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("异常测试房型");
        request.setRackRate(new BigDecimal("200"));
        request.setBedType("大床");
        request.setWindowType("有窗");
        request.setNonSmoking(1);
        request.setMaxGuests(2);
        MvcResult result = mockMvc.perform(post("/api/v1/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private Long createRoom(Long typeId, String roomNo) throws Exception {
        RoomRequest request = new RoomRequest();
        request.setRoomNo(roomNo);
        request.setRoomTypeId(typeId);
        request.setFloorNo(7);
        MvcResult result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
