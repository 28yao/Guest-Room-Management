-- 一次性数据修复：全部脏房置空净、清空保洁任务（开发/演示环境维护用）
-- 执行前请确认当前为开发/测试库，勿在生产环境未经审批执行

-- 保洁态：DIRTY -> CLEAN（空净）
UPDATE room
SET clean_status = 'CLEAN',
    version      = version + 1
WHERE clean_status = 'DIRTY';

-- 兼容迁移前占用态仍为 DIRTY / VACANT_CLEAN 的客房
UPDATE room
SET status       = 'VACANT',
    clean_status = 'CLEAN',
    version      = version + 1
WHERE status IN ('DIRTY', 'VACANT_CLEAN');

-- 清空保洁任务
DELETE FROM hk_task;
