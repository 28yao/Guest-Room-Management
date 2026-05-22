package com.hotel.grms.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.grms.module.auth.dto.LoginRequest;
import com.hotel.grms.module.billing.dto.AdjustPriceRequest;
import com.hotel.grms.module.billing.dto.CheckInPaymentItem;
import com.hotel.grms.module.reservation.dto.AssignRoomRequest;
import com.hotel.grms.module.reservation.dto.ReleaseReservationRequest;
import com.hotel.grms.module.reservation.dto.ReservationCreateRequest;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.shift.dto.ShiftCloseRequest;
import com.hotel.grms.module.stay.dto.ChangeRoomRequest;
import com.hotel.grms.module.stay.dto.CheckInFromReservationRequest;
import com.hotel.grms.module.stay.dto.WalkInCheckInRequest;
import com.hotel.grms.support.GrmsTestDataCleaner;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MOD-QA 全流程验收集成测试（plan TC-01～TC-10）。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GrmsAcceptanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String frontToken;
    private int roomSeq;

    @BeforeEach
    void setUp() throws Exception {
        GrmsTestDataCleaner.cleanTransactionalData(jdbcTemplate);
        roomSeq++;
        adminToken = login("admin");
        frontToken = login("front");
    }

    /** TC-01：预排房 RESERVED；重叠预排 40002。 */
    @Test
    @Order(1)
    void tc01_assignRoomAndRejectOverlap() throws Exception {
        Long typeId = createRoomType("QA标准", new BigDecimal("300"));
        Long roomId = createRoom(typeId, "QA01");
        Long res1 = createReservation(typeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        assignRoom(res1, roomId);
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("RESERVED"));
        Long res2 = createReservation(typeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + res2 + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(40002));
    }

    /** TC-02：预订入住 OCCUPIED，预订 CHECKED_IN。 */
    @Test
    @Order(2)
    void tc02_checkInFromReservation() throws Exception {
        openShift();
        Long typeId = createRoomType("QA预订", new BigDecimal("388"));
        Long roomId = createRoom(typeId, "QA02");
        Long reservationId = createReservation(typeId, LocalDate.now(), LocalDate.now().plusDays(1));
        assignRoom(reservationId, roomId);
        CheckInFromReservationRequest checkIn = new CheckInFromReservationRequest();
        checkIn.setReservationId(reservationId);
        checkIn.setRoomId(roomId);
        checkIn.setPayments(singlePayment("CASH", new BigDecimal("388")));
        mockMvc.perform(post("/api/v1/stays/check-in-from-reservation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_HOUSE"));
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data[?(@.id==" + roomId + ")].status").value("OCCUPIED"));
        String resStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservation WHERE id = ?", String.class, reservationId);
        assertEquals("CHECKED_IN", resStatus);
    }

    /** TC-03：Walk-in 无预订入住。 */
    @Test
    @Order(3)
    void tc03_walkInSuccess() throws Exception {
        openShift();
        Long typeId = createRoomType("QA散客", new BigDecimal("200"));
        Long roomId = createRoom(typeId, "QA03");
        WalkInCheckInRequest checkIn = walkIn(roomId, "QA散客", LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("200"), new BigDecimal("200"));
        mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("IN_HOUSE"));
    }

    /** TC-04：换房后账单按新房型整段重算。 */
    @Test
    @Order(4)
    void tc04_changeRoomRecalculatesFolio() throws Exception {
        openShift();
        Long cheapType = createRoomType("QA经济", new BigDecimal("100"));
        Long deluxeType = createRoomType("QA豪华", new BigDecimal("500"));
        Long roomA = createRoom(cheapType, "QA04A");
        Long roomB = createRoom(deluxeType, "QA04B");
        WalkInCheckInRequest checkIn = walkIn(roomA, "换房客", LocalDate.now(), LocalDate.now().plusDays(2),
                null, new BigDecimal("200"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long stayId = objectMapper.readTree(stayRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        ChangeRoomRequest change = new ChangeRoomRequest();
        change.setTargetRoomId(roomB);
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/change-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(change)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/folios/by-stay/" + stayId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(1000));
    }

    /** TC-05：前台无改价权限 403。 */
    @Test
    @Order(5)
    void tc05_adjustPriceForbiddenForFrontDesk() throws Exception {
        openShift();
        Long typeId = createRoomType("QA改价拒", new BigDecimal("200"));
        Long roomId = createRoom(typeId, "QA05");
        WalkInCheckInRequest checkIn = walkIn(roomId, "改价拒", LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("200"), new BigDecimal("200"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long folioId = objectMapper.readTree(stayRes.getResponse().getContentAsString())
                .path("data").path("folioId").asLong();
        AdjustPriceRequest adjust = new AdjustPriceRequest();
        adjust.setAgreedDailyRate(new BigDecimal("250"));
        mockMvc.perform(post("/api/v1/folios/" + folioId + "/adjust-price")
                        .header("Authorization", "Bearer " + frontToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjust)))
                .andExpect(status().isForbidden());
    }

    /** TC-06：授权改价成功且审计留痕。 */
    @Test
    @Order(6)
    void tc06_adjustPriceWithAuditTrail() throws Exception {
        openShift();
        Long typeId = createRoomType("QA改价成", new BigDecimal("200"));
        Long roomId = createRoom(typeId, "QA06");
        WalkInCheckInRequest checkIn = walkIn(roomId, "改价成", LocalDate.now(), LocalDate.now().plusDays(2),
                new BigDecimal("200"), new BigDecimal("400"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long folioId = objectMapper.readTree(stayRes.getResponse().getContentAsString())
                .path("data").path("folioId").asLong();
        AdjustPriceRequest adjust = new AdjustPriceRequest();
        adjust.setAgreedDailyRate(new BigDecimal("260"));
        mockMvc.perform(post("/api/v1/folios/" + folioId + "/adjust-price")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjust)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("bizType", "FOLIO")
                        .param("operationType", "FOLIO_ADJUST_PRICE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.records[0].beforeValue").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].afterValue").isNotEmpty());
    }

    /** TC-07：入住结清后退房释放，占用空+保洁脏+hk_task。 */
    @Test
    @Order(7)
    void tc07_checkoutCreatesDirtyRoomAndHkTask() throws Exception {
        openShift();
        Long typeId = createRoomType("QA退房", new BigDecimal("150"));
        Long roomId = createRoom(typeId, "QA07");
        WalkInCheckInRequest checkIn = walkIn(roomId, "退房客", LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("150"), new BigDecimal("150"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long stayId = objectMapper.readTree(stayRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CHECKED_OUT"));
        String occupancy = jdbcTemplate.queryForObject("SELECT status FROM room WHERE id = ?", String.class, roomId);
        String clean = jdbcTemplate.queryForObject("SELECT clean_status FROM room WHERE id = ?", String.class, roomId);
        assertEquals("VACANT", occupancy);
        assertEquals("DIRTY", clean);
        Long hkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hk_task WHERE room_id = ? AND status = 'PENDING'",
                Long.class, roomId);
        assertNotNull(hkCount);
        assertTrue(hkCount > 0);
    }

    /** TC-08：保洁完成后空净。 */
    @Test
    @Order(8)
    void tc08_housekeepingCompleteSetsVacantClean() throws Exception {
        openShift();
        Long typeId = createRoomType("QA保洁", new BigDecimal("120"));
        Long roomId = createRoom(typeId, "QA08");
        WalkInCheckInRequest checkIn = walkIn(roomId, "保洁客", LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("120"), new BigDecimal("120"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long stayId = objectMapper.readTree(stayRes.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        Long taskId = jdbcTemplate.queryForObject(
                "SELECT id FROM hk_task WHERE room_id = ? AND status = 'PENDING'",
                Long.class, roomId);
        mockMvc.perform(post("/api/v1/hk/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        assertEquals("VACANT", jdbcTemplate.queryForObject("SELECT status FROM room WHERE id = ?", String.class, roomId));
        assertEquals("CLEAN", jdbcTemplate.queryForObject("SELECT clean_status FROM room WHERE id = ?", String.class, roomId));
    }

    /** TC-09：手动释放预订恢复空房并留审计。 */
    @Test
    @Order(9)
    void tc09_releaseReservationRestoresVacantAndLogs() throws Exception {
        Long typeId = createRoomType("QA释放", new BigDecimal("280"));
        Long roomId = createRoom(typeId, "QA09");
        Long reservationId = createReservation(typeId, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3));
        assignRoom(reservationId, roomId);
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
        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("bizType", "RESERVATION")
                        .param("operationType", "RES_RELEASE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.records[0].bizId").value(reservationId.intValue()));
    }

    /** TC-10：交班单收款汇总与结班。 */
    @Test
    @Order(10)
    void tc10_shiftHandoverPaymentSummary() throws Exception {
        MvcResult openRes = mockMvc.perform(post("/api/v1/shifts/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long shiftId = objectMapper.readTree(openRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        Long typeId = createRoomType("QA交班", new BigDecimal("180"));
        Long roomId = createRoom(typeId, "QA10");
        WalkInCheckInRequest checkIn = walkIn(roomId, "交班客", LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("180"), new BigDecimal("180"));
        MvcResult stayRes = mockMvc.perform(post("/api/v1/stays/walk-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long stayId = objectMapper.readTree(stayRes.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/shifts/" + shiftId + "/handover-preview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.cashTotal").value(180));
        mockMvc.perform(post("/api/v1/stays/" + stayId + "/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        Long hkTaskId = jdbcTemplate.queryForObject(
                "SELECT id FROM hk_task WHERE room_id = ? AND status = 'PENDING'",
                Long.class, roomId);
        mockMvc.perform(post("/api/v1/hk/tasks/" + hkTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
        ShiftCloseRequest close = new ShiftCloseRequest();
        close.setForceClose(false);
        mockMvc.perform(post("/api/v1/shifts/" + shiftId + "/close")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(close)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cashTotal").value(180));
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

    private void openShift() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/open").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createRoomType(String name, BigDecimal rackRate) throws Exception {
        RoomTypeRequest request = new RoomTypeRequest();
        request.setName(name + roomSeq);
        request.setRackRate(rackRate);
        request.setBedType("双床");
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
        request.setRoomNo(roomNo + roomSeq);
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

    private Long createReservation(Long typeId, LocalDate arrival, LocalDate departure) throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setGuestName("QA预订");
        request.setGuestPhone("13800008888");
        request.setRoomTypeId(typeId);
        request.setArrivalDate(arrival);
        request.setDepartureDate(departure);
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void assignRoom(Long reservationId, Long roomId) throws Exception {
        AssignRoomRequest assign = new AssignRoomRequest();
        assign.setRoomId(roomId);
        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/assign-room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private WalkInCheckInRequest walkIn(Long roomId, String guest, LocalDate arrival, LocalDate departure,
                                        BigDecimal dailyRate, BigDecimal payAmount) {
        WalkInCheckInRequest request = new WalkInCheckInRequest();
        request.setRoomId(roomId);
        request.setGuestName(guest);
        request.setGuestPhone("13800006666");
        request.setArrivalDate(arrival);
        request.setDepartureDate(departure);
        request.setAgreedDailyRate(dailyRate);
        request.setPayments(singlePayment("CASH", payAmount));
        return request;
    }

    private List<CheckInPaymentItem> singlePayment(String method, BigDecimal amount) {
        List<CheckInPaymentItem> list = new ArrayList<CheckInPaymentItem>();
        CheckInPaymentItem item = new CheckInPaymentItem();
        item.setMethod(method);
        item.setAmount(amount);
        list.add(item);
        return list;
    }
}
