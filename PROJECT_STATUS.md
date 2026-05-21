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
| 最后更新 | 2026-05-21（文档对齐：MOD-BILL + 房态图增强） |

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
| MOD-HK | 保洁 | 脏房任务、完成置空净 |
| MOD-SHIFT | 交班 | 开班/结班、收款汇总、待办交接 |
| MOD-STAT | 轻量统计 | 出租率、房费营收 |
| MOD-AUDIT | 审计 | 订单全生命周期操作日志 |

**核心数据流**

```
预订(可选预排房) → 入住(在住单+账单，入住时结清) → 换房(整段重算) → 退房(仅释放) → 脏房 → 保洁完成 → 空净
         ↑                                                                              ↓
    手动释放/取消                                                              交班汇总收款与待办
```

**代码现状**：MOD-INFRA、MOD-AUTH、MOD-ROOM、MOD-RES、MOD-STAY、**MOD-BILL（首批）** 已完成；**MOD-HK** 仅退房置脏后 `createTaskOnDirty`（无列表/完成 API）；下一完整模块 **MOD-HK** → **MOD-SHIFT 结班**。

### 近期交付摘要（MOD-BILL + 房态图）

- **入住时结账**（BR-07）：Walk-in / 预订入住 / 房态图快速 Walk-in、**预订入住** 须 `payments[]` 收齐后关账
- **退房**：`POST /stays/{id}/checkout` 仅释放客房；占用态空、保洁态脏；`hk_task` PENDING
- **Folio**：`GET /folios/by-stay/{id}`、改价、分笔支付；`FolioControllerTest` 覆盖 40004
- 在住 / 房态图：**退房**（释放）与 **退款**（提前结束+退费）分离；已移除独立退房结账页
- **交班基础**：`POST/GET /shifts` 开班与当前班（结班预览待 MOD-SHIFT）

### 历史交付（MOD-STAY / MOD-ROOM 增强）

- 办理入住 `/check-in`：Walk-in + 预订入住 Tab；在住 `/in-house`
- 房态图：查看日日程；快速预订 / Walk-in（含结账）；订单行 **预订入住**、换房、退款；快捷 **退房**、净/脏切换
- 预订退订退款 `cancel-with-refund`（V11）；占用态+保洁态双维（V14）

### 历史交付（MOD-RES）

- 预订 CRUD、分页筛选；预排房（房态 → `RESERVED`）；取消/手动释放（可选 No-show）
- 可售校验 `GET /reservations/availability`；超售返回 **40002**
- 前端 `/reservations` 预订管理页（`reservation:manage`）

### 历史交付

- 房态图 **指定日期查看**：`GET /rooms/board?date=yyyy-MM-dd`
- 用户改密/删除、角色与直授恢复默认；房态置脏/置净、强制改态

## 里程碑进度

| 里程碑 | 状态 | 说明 |
|--------|------|------|
| 需求澄清与 spec.md | 已完成 | spec v1.0 |
| 技术方案 plan.md | 已完成 | 含 API/表结构/状态机 |
| 项目管理文档 | 已完成 | README、PROJECT_STATUS、手动验收清单 |
| 数据库与工程骨架（T-INFRA） | 已完成 | V1～V14 迁移脚本（旧库见 README §数据库） |
| 认证与权限（MOD-AUTH） | 已完成 | JWT、RBAC、改密、恢复默认 |
| 客房房态（MOD-ROOM） | 已完成 | 房态图、CRUD、维修、置脏/置净、强改 |
| 预订管理（MOD-RES） | 已完成 | 手工预订、预排房、释放、可售校验 |
| 入住与在住（MOD-STAY） | 已完成 | 含房态图快捷入住/预订入住/退房/退款 |
| 账单退房（MOD-BILL） | 已完成（首批） | 入住结账；退房仅释放；改价/支付 API |
| 保洁（MOD-HK） | 进行中（骨架） | 退房已写 `hk_task`；列表与完成待开发 |
| 交班（MOD-SHIFT） | 部分 | 开班/当前班已有；结班预览与关闭待开发 |
| 统计 / 审计 / QA | 待开始 | MOD-STAT → MOD-AUDIT → T-QA |
| 集成测试与验收（T-QA） | 待开始 | TC-01～12 |

## 风险与阻塞点

| 类型 | 描述 | 缓解措施 |
|------|------|----------|
| 缺陷 | **BUG-AUTH-01**：角色/直授后用户管理前端仍无权限 | 未修复，后期处理；用户管理用 `admin` |
| 进度 | MOD-HK 列表/完成、MOD-SHIFT 结班待开发 | 按 tasks.md §7→§8 |
| 环境 | 旧库缺列导致 50002 | README §数据库：V3～V14（房态图常见缺 **V14** `clean_status`；入住 V9、换房 V10） |
| 业务 | OQ-01～05 已在 plan 锁定 | 变更先改 spec |
| 质量 | 房态并发与超售 | 乐观锁 + 可售校验（预订已接入） |

## 下次更新时间

**MOD-HK 保洁页落地后**，或 **2026-05-28** 例行同步。
