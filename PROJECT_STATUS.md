# 项目状态总览

## 项目基本信息

| 项 | 内容 |
|----|------|
| 项目名称 | 酒店客房管理系统（GRMS） |
| 版本 | MVP v0.1.0（需求/设计 v1.0） |
| 负责人 | liuxinsi |
| 需求基线 | [specs/spec.md](./specs/spec.md) v1.0 |
| 技术方案 | [specs/plan.md](./specs/plan.md) v1.0 |
| 任务清单 | [specs/tasks.md](./specs/tasks.md) |
| 手动验收 | [docs/MANUAL_ACCEPTANCE.md](./docs/MANUAL_ACCEPTANCE.md) |
| 最后更新 | 2026-05-22（MOD-HK / MOD-SHIFT / MOD-STAT；文档与 V16/V17 对齐） |

### 文档对齐关系

| 文档 | 与代码对齐要点 |
|------|----------------|
| [README](./README.md) | 模块进度表、SQL V1～V17、菜单路径与权限码、快速验证步骤 |
| [tasks.md](./specs/tasks.md) | 任务状态与 §1～§9 模块；附录执行顺序 |
| [plan.md](./specs/plan.md) | API/权限码/前端路由（§4.2、§6、§7） |
| [MANUAL_ACCEPTANCE](./docs/MANUAL_ACCEPTANCE.md) | 已交付 §一～§九；未交付 MOD-AUDIT |
| [spec.md](./specs/spec.md) | 业务需求基线（变更时先改此文） |

## 整体架构快照

**形态**：单体前后端分离（Vue3 + Spring Boot + MySQL）

**核心模块**

| 模块 ID | 名称 | 职责 |
|---------|------|------|
| MOD-AUTH/RBAC | 认证与权限 | 登录、JWT、用户/角色/直授权限、改密、删用户、恢复默认 |
| MOD-ROOM | 客房房态 | 房型、客房、房态图；**占用态+保洁态双维**（V14）；维修、净/脏切换、强制改态、按日期查看 |
| MOD-RES | 预订 | 手工预订、预排房、释放、可售校验 |
| MOD-STAY | 入住 | Walk-in、预订入住、换房 |
| MOD-BILL | 账单退房 | 按晚计价、改价、分笔支付、结账 |
| MOD-HK | 保洁 | 脏房任务列表、完成置空净；保洁角色不可看房态图/在住 |
| MOD-SHIFT | 交班 | 开班/结班、本班收款汇总、待办交接（有待办阻断或强制结班） |
| MOD-STAT | 轻量统计 | 出租率快照、区间营收（payment 净额） |
| MOD-AUDIT | 审计 | 订单全生命周期操作日志 |

**核心数据流**

```
预订(可选预排房) → 入住(在住单+账单，入住时结清) → 换房(整段重算) → 退房(仅释放) → 脏房 → 保洁完成 → 空净
         ↑                                                                              ↓
    手动释放/取消                                                              交班汇总收款与待办
```

**代码现状**：MOD-INFRA～MOD-STAT 首批已完成；下一模块 **MOD-AUDIT** → **T-QA**。

### 近期交付摘要（MOD-HK + MOD-SHIFT + MOD-STAT）

- **保洁**：`GET /hk/tasks`、`POST /hk/tasks/{id}/complete`；退房/置脏自动生成 `hk_task`；前端 `/housekeeping`；`hk01` 仅保洁菜单（**V16** 权限隔离：无房态图/在住）
- **交班**：`GET /shifts/{id}/handover-preview`、`POST /shifts/{id}/close`；现金/微信/支付宝汇总；待办阻断结班；`shift:force_close` 可强制结班；前端 `/shift`（**V17** 结班表列对齐）
- **统计**：`GET /stats/occupancy`、`GET /stats/revenue`；前端 `/stats`（`stat:view`）

### 历史交付（MOD-BILL + 房态图）

- **入住时结账**（BR-07）：Walk-in / 预订入住 / 房态图快速 Walk-in、**预订入住** 须 `payments[]` 收齐后关账
- **退房**：`POST /stays/{id}/checkout` 仅释放客房；占用态空、保洁态脏；`hk_task` PENDING
- 在住 / 房态图：**退房**（释放）与 **退款**（提前结束+退费）分离

### 历史交付（MOD-STAY / MOD-ROOM 增强）

- 办理入住 `/check-in`：Walk-in + 预订入住 Tab；在住 `/in-house`
- 房态图：查看日日程；快速预订 / Walk-in（含结账）；订单行 **预订入住**、换房、退款；快捷 **退房**、净/脏切换
- 预订退订退款 `cancel-with-refund`（V11）；占用态+保洁态双维（V14）

## 里程碑进度

| 里程碑 | 状态 | 说明 |
|--------|------|------|
| 需求澄清与 spec.md | 已完成 | spec v1.0 |
| 技术方案 plan.md | 已完成 | 含 API/表结构/状态机 |
| 项目管理文档 | 已完成 | README、PROJECT_STATUS、手动验收清单 |
| 数据库与工程骨架（T-INFRA） | 已完成 | V1～V17 迁移脚本（旧库见 README §数据库） |
| 认证与权限（MOD-AUTH） | 已完成 | JWT、RBAC、改密、恢复默认 |
| 客房房态（MOD-ROOM） | 已完成 | 房态图、CRUD、维修、置脏/置净、强改 |
| 预订管理（MOD-RES） | 已完成 | 手工预订、预排房、释放、可售校验 |
| 入住与在住（MOD-STAY） | 已完成 | 含房态图快捷入住/预订入住/退房/退款 |
| 账单退房（MOD-BILL） | 已完成（首批） | 入住结账；退房仅释放；改价/支付 API |
| 保洁（MOD-HK） | 已完成（首批） | 任务列表/完成；保洁角色隔离 |
| 交班（MOD-SHIFT） | 已完成（首批） | 开班/预览/结班/待办阻断 |
| 统计（MOD-STAT） | 已完成（首批） | `/stats` 出租率+区间营收 |
| 审计 / QA | 待开始 | MOD-AUDIT → T-QA |
| 集成测试与验收（T-QA） | 待开始 | TC-01～12 |

## 风险与阻塞点

| 类型 | 描述 | 缓解措施 |
|------|------|----------|
| 缺陷 | **BUG-AUTH-01**：角色/直授后用户管理前端仍无权限 | 未修复，后期处理；用户管理用 `admin` |
| 环境 | 旧库缺列导致 50002 | README §数据库：V3～V17 |
| 业务 | OQ-01～05 已在 plan 锁定 | 变更先改 spec |
| 质量 | 房态并发与超售 | 乐观锁 + 可售校验（预订已接入） |

## 下次更新时间

**MOD-AUDIT 审计落地后**，或 **2026-05-28** 例行同步。
