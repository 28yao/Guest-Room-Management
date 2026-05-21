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
| 最后更新 | 2026-05-21（MOD-AUTH 已完成） |

## 整体架构快照

**形态**：单体前后端分离（Vue3 + Spring Boot + MySQL）

**核心模块**

| 模块 ID | 名称 | 职责 |
|---------|------|------|
| MOD-AUTH/RBAC | 认证与权限 | 登录、JWT、角色/敏感权限下放 |
| MOD-ROOM | 客房房态 | 房型、客房、房态图、维修、强制改态 |
| MOD-RES | 预订 | 手工预订、预排房、释放、可售校验 |
| MOD-STAY | 入住 | Walk-in、预订入住、换房 |
| MOD-BILL | 账单退房 | 按晚计价、改价、分笔支付、结账 |
| MOD-HK | 保洁 | 脏房任务、完成置空净 |
| MOD-SHIFT | 交班 | 开班/结班、收款汇总、待办交接 |
| MOD-STAT | 轻量统计 | 出租率、房费营收 |
| MOD-AUDIT | 审计 | 订单全生命周期操作日志 |

**核心数据流**

```
预订(可选预排房) → 入住(在住单+账单) → 换房(整段重算) → 退房结清 → 脏房 → 保洁完成 → 空净
         ↑                                                              ↓
    手动释放/取消                                              交班汇总收款与待办
```

**代码现状**：MOD-INFRA、**MOD-AUTH（JWT+RBAC）** 已完成；下一模块 MOD-ROOM。

## 里程碑进度

| 里程碑 | 状态 | 说明 |
|--------|------|------|
| 需求澄清与 spec.md | 已完成 | spec v1.0 已评审基线 |
| 技术方案 plan.md | 已完成 | 含 API/表结构/状态机/任务映射 |
| 项目管理文档 | 已完成 | PROJECT_STATUS、README、tasks.md |
| 数据库与工程骨架（T-INFRA） | 已完成 | `sql/V1`、`V2`；Spring Boot + Vue3 工程 |
| 认证与权限（MOD-AUTH） | 已完成 | JWT 登录、用户/角色/直授权限 |
| 后端 MVP 其余模块 | 待开始 | 见 tasks.md §3 起 |
| 前端 MVP 页面（T-FE） | 待开始 | 见 tasks.md |
| 集成测试与验收（T-QA） | 待开始 | TC-01～12 |

## 风险与阻塞点

| 类型 | 描述 | 缓解措施 |
|------|------|----------|
| 进度 | 业务模块未启动 | 按 tasks.md 从 MOD-AUTH（§2）执行 |
| 业务 | OQ-01～05 已在 plan 锁定默认值 | 变更先改 spec.md 再改 plan/tasks |
| 范围 | 会员/看板/OTA 易渗入 MVP | 以 spec §2.3 排除项为准 |
| 质量 | 房态并发与超售 | 乐观锁 + RoomAvailabilityService（plan §3） |
| 协作 | 环境未统一 | README 快速开始落地后补充实际端口与账号 |

## 下次更新时间

**2026-05-28**（或完成 T-INFRA-01 / T-AUTH-01 后即时更新）
