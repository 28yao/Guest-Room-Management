# 酒店客房管理系统（GRMS）

单体酒店 Web 端客房管理 MVP：房态 → 预订 → 入住 → 退房 → 保洁 → 交班，含 RBAC 与操作审计。

## 文档索引

| 文档 | 说明 |
|------|------|
| [specs/spec.md](./specs/spec.md) | 产品需求规格（业务基线） |
| [specs/plan.md](./specs/plan.md) | 技术实现方案（API/库表/状态机） |
| [specs/tasks.md](./specs/tasks.md) | 可执行原子任务清单 |
| [PROJECT_STATUS.md](./PROJECT_STATUS.md) | 项目状态与里程碑 |
| [AGENTS.md](./AGENTS.md) | 技术栈与编码约束 |

## 项目目录结构

```
Guest Room Management/
├── AGENTS.md                 # 技术栈与规范
├── README.md                 # 本文件
├── PROJECT_STATUS.md         # 状态总览
├── specs/
│   ├── spec.md               # 需求规格
│   ├── plan.md               # 技术方案
│   └── tasks.md              # 任务清单
├── sql/
│   ├── V1__init_schema.sql   # 建表（仅 CREATE）
│   └── V2__seed_data.sql     # 种子数据（admin/admin123）
├── backend/                  # Spring Boot 2.7
│   └── src/main/java/com/hotel/grms/
└── frontend/                 # Vue3 + Element Plus
    └── src/
```

## 快速开始

> MOD-INFRA 已完成；启动前请先执行 SQL 脚本并配置 `application-dev.yml` 数据库账号。

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
# 若库是早期版本建的（sys_role / sys_permission 无 description 列），再执行：
mysql -u root -p grms < sql/V3__auth_add_description.sql
```

### 后端

**方式 A：MySQL（推荐生产一致）**

1. 创建库并执行 `sql/V1__init_schema.sql`、`sql/V2__seed_data.sql`
2. 在 `backend/` 下复制 `application-local.yml.example` 为 `application-local.yml`，填写真实 MySQL 密码
3. 启动：

```bash
cd backend
mvn spring-boot:run
```

**方式 B：内置 H2（无需 MySQL，快速联调）**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

默认：`http://localhost:8080`

> 若登录返回 500 且日志含 `Access denied for user 'root'`，说明 MySQL 密码与配置不一致，请用方式 A 配置 `application-local.yml`，或改用方式 B。

### 前端

```bash
cd frontend
npm install
npm run dev
```

# 2. 后端（按需改 application-dev.yml 账号密码）
cd backend
mvn spring-boot:run
# 3. 前端
cd frontend
npm run dev

默认：`http://localhost:5173`

### 验证步骤

1. 访问前端登录页，使用种子管理员账号登录（见 `sql` 种子数据说明）。
2. 打开房态图，确认空净房展示正常。
3. 执行一条完整链路：预订预排房 → 入住 → 退房 → 保洁完成 → 房态变空净（对照 [spec §18](./specs/spec.md)）。

## 贡献规范

1. **需求变更**：先更新 `specs/spec.md`，再同步 `plan.md`、`tasks.md`、`PROJECT_STATUS.md`。
2. **任务执行**：以 `specs/tasks.md` 任务编号为准，完成后将状态改为 `已完成` 并更新 PROJECT_STATUS。
3. **Java 代码**：遵守 [AGENTS.md](./AGENTS.md)（JavaDoc、`@author liuxinsi`、禁止嵌套循环、参数化 SQL、禁止 DROP）。
4. **分支与提交**：`feat/<模块>-<简述>`；提交信息说明对应任务编号（如 `T-ROOM-03`）。
5. **危险操作**：生产写操作须二次确认；SQL 脚本禁止 `DROP TABLE/DATABASE`。
