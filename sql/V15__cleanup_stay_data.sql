-- 清理在住管理相关事务数据（开发/验收后重置用）
-- 保留：用户权限、房型客房、未入住预订、开班记录等
-- 执行前请确认环境为开发/测试库，勿在生产库执行
-- 依赖：V1 及后续迁移已执行

SET NAMES utf8mb4;

-- 支付流水（在住账单）
DELETE p FROM payment p
INNER JOIN folio f ON p.folio_id = f.id;

-- 账单明细与主表
DELETE fl FROM folio_line fl
INNER JOIN folio f ON fl.folio_id = f.id;

DELETE FROM folio;

-- 在住客人与订单
DELETE FROM stay_guest;
DELETE FROM stay_order;

-- 退房产生的保洁任务
DELETE FROM hk_task;

-- 恢复客房占用态（OCCUPIED -> VACANT，保洁态不变）
UPDATE room SET status = 'VACANT' WHERE status = 'OCCUPIED';

-- 已入住预订恢复为已确认，便于再次办理入住
UPDATE reservation SET status = 'CONFIRMED' WHERE status = 'CHECKED_IN';
