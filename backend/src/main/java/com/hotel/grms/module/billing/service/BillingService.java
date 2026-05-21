package com.hotel.grms.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.billing.FolioStatus;
import com.hotel.grms.module.billing.entity.Folio;
import com.hotel.grms.module.billing.entity.FolioLine;
import com.hotel.grms.module.billing.mapper.FolioLineMapper;
import com.hotel.grms.module.billing.mapper.FolioMapper;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.service.RoomTypeService;
import com.hotel.grms.module.stay.entity.StayOrder;
import com.hotel.grms.module.stay.mapper.StayOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 账单服务：创建账单、按晚生成房费行、整段重算（换房）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class BillingService {

    private static final String LINE_TYPE_ROOM = "ROOM";

    private final FolioMapper folioMapper;
    private final FolioLineMapper folioLineMapper;
    private final StayOrderMapper stayOrderMapper;
    private final RoomTypeService roomTypeService;

    public BillingService(FolioMapper folioMapper, FolioLineMapper folioLineMapper,
                          StayOrderMapper stayOrderMapper, RoomTypeService roomTypeService) {
        this.folioMapper = folioMapper;
        this.folioLineMapper = folioLineMapper;
        this.stayOrderMapper = stayOrderMapper;
        this.roomTypeService = roomTypeService;
    }

    /**
     * 为在住单创建空账单并生成按晚房费明细。
     *
     * @param stayOrder 在住单
     * @return 账单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long initFolioForStay(StayOrder stayOrder) {
        Folio folio = new Folio();
        folio.setStayOrderId(stayOrder.getId());
        folio.setTotalAmount(BigDecimal.ZERO);
        folio.setPaidAmount(BigDecimal.ZERO);
        folio.setStatus(FolioStatus.OPEN);
        folioMapper.insert(folio);
        regenerateActiveLines(folio.getId(), stayOrder);
        return folio.getId();
    }

    /**
     * 按当前在住单房型与房价整段重算账单（换房后调用，BR-06）。
     *
     * @param stayOrderId 在住单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recalculateFullStay(Long stayOrderId) {
        StayOrder stayOrder = stayOrderMapper.selectById(stayOrderId);
        if (stayOrder == null) {
            throw new BusinessException(40015, "在住单不存在");
        }
        Folio folio = findFolioByStay(stayOrderId);
        if (folio == null) {
            throw new BusinessException(40016, "账单不存在");
        }
        regenerateActiveLines(folio.getId(), stayOrder);
    }

    private void regenerateActiveLines(Long folioId, StayOrder stayOrder) {
        folioLineMapper.update(null, new LambdaUpdateWrapper<FolioLine>()
                .eq(FolioLine::getFolioId, folioId)
                .eq(FolioLine::getActive, 1)
                .set(FolioLine::getActive, 0));

        BigDecimal dailyRate = resolveDailyRate(stayOrder);
        int nights = computeNights(stayOrder.getArrivalDate(), stayOrder.getDepartureDate());
        BigDecimal total = BigDecimal.ZERO;
        List<FolioLine> lines = new ArrayList<FolioLine>();
        LocalDate nightDate = stayOrder.getArrivalDate();
        for (int index = 0; index < nights; index++) {
            FolioLine line = new FolioLine();
            line.setFolioId(folioId);
            line.setLineType(LINE_TYPE_ROOM);
            line.setDescription("房费 " + nightDate);
            line.setQuantity(1);
            line.setUnitPrice(dailyRate);
            line.setAmount(dailyRate);
            line.setActive(1);
            lines.add(line);
            total = total.add(dailyRate);
            nightDate = nightDate.plusDays(1);
        }
        for (FolioLine line : lines) {
            folioLineMapper.insert(line);
        }
        Folio update = new Folio();
        update.setId(folioId);
        update.setTotalAmount(total);
        folioMapper.updateById(update);
    }

    private BigDecimal resolveDailyRate(StayOrder stayOrder) {
        if (stayOrder.getAgreedDailyRate() != null) {
            return stayOrder.getAgreedDailyRate();
        }
        RoomType type = roomTypeService.getById(stayOrder.getRoomTypeId());
        return type.getRackRate();
    }

    private int computeNights(LocalDate arrival, LocalDate departure) {
        long nights = ChronoUnit.DAYS.between(arrival, departure);
        if (nights < 1) {
            nights = 1;
        }
        return (int) nights;
    }

    private Folio findFolioByStay(Long stayOrderId) {
        return folioMapper.selectOne(new LambdaQueryWrapper<Folio>().eq(Folio::getStayOrderId, stayOrderId));
    }
}
