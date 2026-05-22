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

## 当前进度（截至 2026-05-22）

| 模块 | 状态 | 说明 |
|------|------|------|
| MOD-INFRA | 已完成 | 工程骨架、SQL V1～V18、健康检查 |
| MOD-AUTH | 已完成 | 登录/JWT/RBAC；用户改密/删除、角色/直授 **恢复默认** |
| MOD-ROOM | 已完成 | 房态图双维标签；房型/客房；维修；净/脏切换（V14）；强制改态 |
| MOD-RES | 已完成 | 预订 CRUD、预排房、释放/取消、退订退款（V11）、可售查询 |
| MOD-STAY | 已完成 | 办理入住、在住管理；房态图快速预订/Walk-in/**预订入住**/退房/退款/换房 |
| MOD-BILL | 已完成（首批） | 入住时结清房费；改价/支付；退房仅释放客房 |
| MOD-HK | 已完成（首批） | 保洁任务列表/完成；`hk01` 不可看房态图、在住（V16） |
| MOD-SHIFT | 已完成（首批） | 开班/结班预览/关闭；待办阻断与强制结班 |
| MOD-STAT | 已完成（首批） | 出租率快照、区间营收；`/stats`（`stat:view`） |
| MOD-AUDIT | 已完成（首批） | 操作审计 `/system/audit`（`audit:view`） |
| MOD-QA | 已完成（首批） | `mvn test` 覆盖 plan TC-01～12；可选 E2E 未做 |

**默认账号**（`sql/V2__seed_data.sql`）：`admin` / `admin123`（管理员）；`hk01` / `admin123`（保洁，仅保洁任务）

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
│   ├── V10__folio_timestamps.sql     # 账单主表 created_at/updated_at（换房重算）
│   ├── V11__payment_refund.sql       # 支付流水 folio 可空（预订退款）
│   ├── V12__front_desk_room_clean.sql   # 前台置净权限
│   ├── V13__restore_room_clean_dirty_perms.sql  # 恢复净/脏切换权限
│   ├── V14__room_clean_status.sql    # 保洁态 clean_status（房态图必跑，执行后重启后端）
│   ├── V15__cleanup_stay_data.sql    # 可选：开发库清理在住/账单/保洁任务（勿用于生产）
│   ├── V16__room_board_in_house_perms.sql  # 房态图/在住查看权（保洁不授予）
│   └── V17__shift_handover_align.sql # 结班表汇总列对齐（交班预览/结班）
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
mysql -u root -p grms < sql/V11__payment_refund.sql
mysql -u root -p grms < sql/V12__front_desk_room_clean.sql
mysql -u root -p grms < sql/V13__restore_room_clean_dirty_perms.sql
mysql -u root -p grms < sql/V14__room_clean_status.sql
mysql -u root -p grms < sql/V16__room_board_in_house_perms.sql
mysql -u root -p grms < sql/V17__shift_handover_align.sql
mysql -u root -p grms < sql/V18__operation_log_align.sql
# 房态图演示数据（可选）：
mysql -u root -p grms < sql/V4__room_seed.sql
```

**Windows PowerShell**（`<` 重定向不可用时可逐条管道执行）：

```powershell
# 含中文的脚本请指定字符集，避免 PowerShell 管道乱码
Get-Content sql/V16__room_board_in_house_perms.sql -Encoding UTF8 -Raw | mysql -u root -p你的密码 --default-character-set=utf8mb4 grms
Get-Content sql/V14__room_clean_status.sql -Encoding UTF8 -Raw | mysql -u root -p你的密码 --default-character-set=utf8mb4 grms
```

**表结构不一致（前端/接口 50002）**：后端日志中 `Unknown column` 指明缺列；常见对照：

| 缺列/现象 | 脚本 |
|-----------|------|
| `sys_role.description` 等 | V3 |
| 早期 `room`/`room_type` 缺列 | V5、V6 |
| `reservation.arrival_at` / `departure_at` | V7 |
| `stay_order.guest_name` | V8 |
| `folio_line.quantity` / `unit_price` | V9 |
| `folio.created_at` / `updated_at` | V10 |
| **`shift_handover.cash_total`（交接班报错，实为结班表）** | **V17** |
| **`operation_log.operation_type`（操作审计页报错，旧库为 action 列）** | **V18** |
| 预订退款 `payment.folio_id` 不可空 | V11 |
| **`room.clean_status`（房态图整页报错）** | **V14**（执行后重启后端） |
| **侧栏无「房态图」「在住管理」/ 接口 403**（保洁除外） | **V16**（补权限点并授予管理员/店长/前台；须在「角色权限」「敏感权限直授」中可见） |

已执行 V3～V10 仍报「表结构不一致」时，优先检查并执行 **V14**；或按上表从 README 顺序补跑 V11～V17。

**开发库重置在住数据**（可选，确认非生产库）：`sql/V15__cleanup_stay_data.sql` 清空在住/账单/保洁任务并恢复空房占用态。

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
| 房态图 | `/rooms/board` | `room:board:view`（**保洁角色无**）；**全部在住/预订** 一键筛选；日程内预订入住/退房等 |
| 客房管理 | `/rooms` | `room:manage` |
| 房型管理 | `/room-types` | `room:type:manage` |
| 用户管理 | `/system/users` | `system:user:manage`；含 **修改密码**、**删除用户** |
| 角色权限 | `/system/roles` | `system:role:manage`；可勾选 `room:board:view`、`stay:in_house:view` 等；含 **恢复默认** |
| 敏感权限直授 | `/system/user-permissions` | `system:permission:grant`；可向单用户直授房态图/在住查看等；含 **恢复默认** |
| 办理入住 | `/check-in` | `stay:checkin`；Walk-in / 预订入住 Tab，**入住时结清房费** |
| 在住管理 | `/in-house` | `stay:in_house:view`（保洁角色无此权限） |
| 预订管理 | `/reservations` | `reservation:manage` |
| 保洁任务 | `/housekeeping` | `hk:view`；`hk:complete` 可完成打扫 |
| 开班结班 | `/shift` | `shift:open` / `shift:close`；预览待办与收款；`shift:force_close` 强制结班 |
| 经营统计 | `/stats` | `stat:view`（店长/管理员；前台默认无） |

### 验证步骤（当前 MVP）

1. 登录 `admin` / `admin123`，确认进入房态图（旧库须已执行 **V16**）。  
2. 系统管理：用户 **修改密码**、**删除**；角色/直授 **恢复默认**（见 [手动验收清单](./docs/MANUAL_ACCEPTANCE.md)）。  
3. 房型/客房 CRUD，房态图筛选、维修、净/脏切换、强制改态。  
4. **开班** → 办理入住或房态图 Walk-in/预订入住（收款=应付）→ 在住 **退房** 或 **退款** → 保洁 **完成打扫** → **结班**（见 MOD-SHIFT）。  
5. 店长/管理员查看 **经营统计** `/stats`（MOD-STAT）。  
6. 完整用例见 [手动验收清单](./docs/MANUAL_ACCEPTANCE.md)（§一～§十）。  
7. **自动化验收**：`cd backend && mvn test`（H2，75 用例；含 `GrmsAcceptanceIntegrationTest` TC-01～10）。

### 后端测试

```bash
cd backend
mvn test
```

| 测试类 | 覆盖 |
|--------|------|
| `GrmsAcceptanceIntegrationTest` | plan TC-01～TC-10 |
| `GrmsExceptionIntegrationTest` | TC-11/TC-12 |
| `BillingServiceNightCountTest` | 按晚计费边界 |
| 各模块 `*ControllerTest` | 模块级 IT |

## 贡献规范

1. **需求变更**：先更新 `specs/spec.md`，再同步 `plan.md`、`tasks.md`、`PROJECT_STATUS.md`、`docs/MANUAL_ACCEPTANCE.md`。  
2. **任务执行**：以 `specs/tasks.md` 任务编号为准，完成后标 `已完成` 并更新 PROJECT_STATUS 与 README 进度表。  
3. **Java 代码**：遵守 [AGENTS.md](./AGENTS.md)（JavaDoc、`@author liuxinsi`、禁止嵌套循环、参数化 SQL、禁止 DROP）。  
4. **分支与提交**：`feat/<模块>-<简述>`；提交信息说明对应任务编号。  
5. **危险操作**：生产写操作须二次确认；SQL 脚本禁止 `DROP TABLE/DATABASE`。
