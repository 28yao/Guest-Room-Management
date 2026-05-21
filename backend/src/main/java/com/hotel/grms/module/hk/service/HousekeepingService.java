package com.hotel.grms.module.hk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.module.hk.HkTaskStatus;
import com.hotel.grms.module.hk.entity.HkTask;
import com.hotel.grms.module.hk.mapper.HkTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 保洁任务服务：退房置脏后创建待打扫任务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class HousekeepingService {

    private final HkTaskMapper hkTaskMapper;

    public HousekeepingService(HkTaskMapper hkTaskMapper) {
        this.hkTaskMapper = hkTaskMapper;
    }

    /**
     * 为客房创建待打扫任务（若已有 PENDING 则跳过）。
     *
     * @param roomId 客房 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTaskOnDirty(Long roomId) {
        Long pending = hkTaskMapper.selectCount(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getRoomId, roomId)
                .eq(HkTask::getStatus, HkTaskStatus.PENDING));
        if (pending != null && pending > 0) {
            return;
        }
        HkTask task = new HkTask();
        task.setRoomId(roomId);
        task.setStatus(HkTaskStatus.PENDING);
        hkTaskMapper.insert(task);
    }
}
