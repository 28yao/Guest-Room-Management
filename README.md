# 酒店客房管理系统（GRMS）

单体酒店 Web 端客房管理 MVP：房态 → 预订 → 入住 → 退房 → 保洁 → 交班，含 RBAC 与操作审计。

## 文档索引

| 文档 | 说明 |
|------|------|
| [specs/spec.md](./specs/spec.md) | 产品需求规格（业务基线） |
| [specs/plan.md](./specs/plan.md) | 技术实现方案（API/库表/状态机） |
| [specs/tasks.md](./specs/tasks.md) | 可执行原子任务清单 |
| [PROJECT_STATUS.md](./PROJECT_STATUS.md) | 项目状态与里程碑 |
| [docs/MANUAL_ACCEPTANCE.md](./docs/MANUAL_ACCEPTANCE.md) | **手动验收清单**（已交付模块） |
| [AGENTS.md](./AGENTS.md) | 技术栈与编码约束 |

## 当前进度（截至 2026-05-21）

| 模块 | 状态 | 说明 |
|------|------|------|
| MOD-INFRA | 已完成 | 工程骨架、SQL、健康检查 |
| MOD-AUTH | 已完成 | 登录/JWT/RBAC；用户改密/删除、角色/直授 **恢复默认** |
| MOD-ROOM | 已完成 | 房态图（含指定日期查看）、房型/客房、维修、置脏/置净、强制改态 |
| MOD-RES | 已完成 | 预订 CRUD、预排房、释放/取消、可售查询 |
| MOD-STAY | 已完成（首批） | Walk-in（可改 18:00/12:00）、预订入住、在住列表、换房、备注 |
| MOD-BILL 及以后 | 待开发 | 见 [tasks.md](./specs/tasks.md) §6 起 |

**默认管理员**：`admin` / `admin123`（`sql/V2__seed_data.sql`）

## 项目目录结构

```
Guest Room Management/
├── AGENTS.md
├── README.md
├── PROJECT_STATUS.md
├── docs/
│   └── MANUAL_ACCEPTANCE.md    # 手动验收清单
├── specs/
│   ├── spec.md
│   ├── plan.md
│   └── tasks.md
├── sql/
│   ├── V1__init_schema.sql     # 建表
│   ├── V2__seed_data.sql       # 种子（角色/权限/admin）
│   ├── V3__auth_add_description.sql
│   ├── V4__room_seed.sql       # 演示房型/客房（可选）
│   ├── V5__schema_align_legacy.sql
│   ├── V6__room_status_dirty_clean.sql
│   ├── V7__reservation_datetime.sql  # 预订时刻 + 默认 18:00/12:00 回填
│   ├── V8__stay_order_guest.sql      # 在住单 guest_name/guest_phone
│   ├── V9__folio_line_billing.sql    # 账单明细 quantity/unit_price
│   └── V10__folio_timestamps.sql     # 账单主表 created_at/updated_at（换房重算）
├── backend/
└── frontend/
```

## 快速开始

### 环境准备

| 依赖 | 版本要求 |
|------|----------|
| JDK | 1.8 |
| Maven | 3.6+ |
| Node.js | 18+ |
| MySQL | 8.0 |

### 数据库

```bash
mysql -u root -p -e "CREATE DATABASE grms DEFAULT CHARSET utf8mb4;"
mysql -u root -p grms < sql/V1__init_schema.sql
mysql -u root -p grms < sql/V2__seed_data.sql
# 早期建库或报「表结构不一致」时追加：
mysql -u root -p grms < sql/V3__auth_add_description.sql
mysql -u root -p grms < sql/V5__schema_align_legacy.sql
mysql -u root -p grms < sql/V6__room_status_dirty_clean.sql
mysql -u root -p grms < sql/V7__reservation_datetime.sql
mysql -u root -p grms < sql/V8__stay_order_guest.sql
mysql -u root -p grms < sql/V9__folio_line_billing.sql
mysql -u root -p grms < sql/V10__folio_timestamps.sql
# 房态图演示数据（可选）：
mysql -u root -p grms < sql/V4__room_seed.sql
```

### 后端

1. 复制 `backend/application-local.yml.example` → `backend/application-local.yml`，填写 MySQL 密码  
2. 启动：

```bash
cd backend
mvn spring-boot:run
```

**H2 快速联调（无需 MySQL）**：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

默认：`http://localhost:8080`

### 前端

```bash
cd frontend
# npm install
npm run dev
```

默认：`http://localhost:5173`（代理 `/api` → 8080）

### 已实现功能速览

| 菜单/页面 | 路径 | 权限要点 |
|-----------|------|----------|
| 房态图 | `/rooms/board` | 登录即可；可按 **查看日期** 看预抵/预离；维修/置脏/置净/强改按权显示 |
| 客房管理 | `/rooms` | `room:manage` |
| 房型管理 | `/room-types` | `room:type:manage` |
| 用户管理 | `/system/users` | `system:user:manage`；含 **修改密码**、**删除用户** |
| 角色权限 | `/system/roles` | `system:role:manage`；含 **恢复默认** |
| 敏感权限直授 | `/system/user-permissions` | `system:permission:grant`；含 **恢复默认** |

### 验证步骤（当前 MVP）

1. 登录 `admin` / `admin123`，确认进入房态图。  
2. 系统管理：用户 **修改密码**、**删除**；角色/直授 **恢复默认**（见 [手动验收清单](./docs/MANUAL_ACCEPTANCE.md)）。  
3. 房型/客房 CRUD，房态图筛选、维修、置脏/置净、强制改态。  
4. 全链路（预订→入住→退房）待 MOD-RES 等模块完成后验收。

## 贡献规范

1. **需求变更**：先更新 `specs/spec.md`，再同步 `plan.md`、`tasks.md`、`PROJECT_STATUS.md`。  
2. **任务执行**：以 `specs/tasks.md` 任务编号为准，完成后标 `已完成` 并更新 PROJECT_STATUS。  
3. **Java 代码**：遵守 [AGENTS.md](./AGENTS.md)（JavaDoc、`@author liuxinsi`、禁止嵌套循环、参数化 SQL、禁止 DROP）。  
4. **分支与提交**：`feat/<模块>-<简述>`；提交信息说明对应任务编号。  
5. **危险操作**：生产写操作须二次确认；SQL 脚本禁止 `DROP TABLE/DATABASE`。
