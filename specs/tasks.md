# 可执行任务清单（MVP）

| 属性 | 值 |
|------|-----|
| 需求基线 | [spec.md](./spec.md) v1.0 |
| 技术方案 | [plan.md](./plan.md) v1.0 |
| 状态图例 | `已完成` / `进行中` / `待开始` |
| 项目管理 | [README](../README.md)、[PROJECT_STATUS](../PROJECT_STATUS.md)、[手动验收](../docs/MANUAL_ACCEPTANCE.md) 与本文档同步更新 |
| 文档同步 | 2026-05-22：已交付至 MOD-STAT；SQL V1～V17；权限 V16（房态图/在住） |

---

## 1. 工程基础与数据库（MOD-INFRA）

**模块目标**：初始化前后端工程、MySQL 表结构与种子数据，支撑后续模块开发。  
**Spec/plan**：plan §1.3、§5、TASK-01

**用户可见内容**：无（基础设施）。

**用户操作流程**：无。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-INFRA-FE-01 | 创建 Vite+Vue3+TS+Element Plus 空工程 | — | 1h | `npm run dev` 可启动 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-INFRA-CTL-01 | 创建 Spring Boot 2.7 工程与统一响应体 `R<T>` | — | 1h | 应用可启动，`/actuator/health` 或根路径 200 | 已完成 |

### Service 层任务

无（本模块无业务 Service）。

### Mapper 层任务

无。

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-INFRA-DB-01 | 编写 `sql/V1__init_schema.sql`（仅 CREATE，含 plan §5.1 全表） | — | 3h | MySQL 执行成功，无 DROP 语句 | 已完成 |
| T-INFRA-DB-02 | 编写种子数据：权限点、角色、管理员账号 | [依赖: T-INFRA-DB-01] | 1h | 管理员可登录（后续 T-AUTH 验证） | 已完成 |
| T-INFRA-DB-04 | 迁移 V16/V17：房态图/在住权限、结班表对齐 | [依赖: T-INFRA-DB-02] | 0.5h | 旧库执行后前台可见房态图；结班不 50002 | 已完成 |
| T-INFRA-DB-03 | 配置 MyBatis-Plus 与 `application-dev.yml` 数据源 | [依赖: T-INFRA-CTL-01, T-INFRA-DB-01] | 1h | 启动连库无报错 | 已完成 |

### 页面测试方法

不适用。

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-INFRA-IT-01 | 健康检查接口冒烟 | [依赖: T-INFRA-CTL-01] | 0.5h | HTTP 200 | 已完成 |

### 异常情况测试

不适用。

**当前状态**：`已完成`

---

## 2. 认证与权限（MOD-AUTH / MOD-RBAC）

**模块目标**：JWT 登录、角色菜单权限、敏感权限直授（含改价下放）。  
**Spec/plan**：spec §3、§10；plan §4、API-AUTH/SYS

**用户可见内容**：登录页；管理员可见用户/角色/权限配置。

**用户操作流程**：输入账号密码登录 → 按角色看到菜单 → 管理员为用户勾选 `billing:price:adjust` 等敏感权。

**已实现增强（文档同步 2026-05-21）**：

- 用户管理：操作列 **修改密码**（`PUT /api/v1/users/{id}/password`）、**删除**（`DELETE /api/v1/users/{id}`）
- 角色权限：**恢复默认**（`POST /api/v1/roles/{id}/permissions/restore-default`，对齐 `V2__seed_data.sql`）
- 敏感权限直授：**恢复默认**（`POST /api/v1/users/{id}/permissions/restore-default`，清空直授）

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-FE-01 | 登录页 `Login.vue` + token 存储 | [依赖: T-INFRA-FE-01] | 2h | 登录成功跳转首页 | 已完成 |
| T-AUTH-FE-02 | Axios 拦截器附加 JWT、401 跳转登录 | [依赖: T-AUTH-FE-01] | 1h | 过期自动登出 | 已完成 |
| T-AUTH-FE-03 | 路由守卫 `meta.permissions` | [依赖: T-AUTH-FE-02] | 2h | 无权限路由不可进入 | 已完成 |
| T-AUTH-FE-04 | 用户管理页：列表/新增/停用/修改密码 | [依赖: T-AUTH-FE-03, T-AUTH-CTL-04] | 3h | CRUD 与改密 | 已完成 |
| T-AUTH-FE-05 | 角色权限配置页（含恢复默认） | [依赖: T-AUTH-CTL-05] | 3h | 勾选保存/恢复种子默认 | 已完成 |
| T-AUTH-FE-06 | 用户敏感权限直授页（含恢复默认） | [依赖: T-AUTH-CTL-06] | 2h | 改价权可授/可撤/清空直授 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-CTL-01 | `POST /api/v1/auth/login` | [依赖: T-INFRA-CTL-01, T-INFRA-DB-01] | 2h | 返回 JWT + 权限码列表 | 已完成 |
| T-AUTH-CTL-02 | `POST /api/v1/auth/logout` | [依赖: T-AUTH-CTL-01] | 1h | 记登录/登出日志（可选） | 已完成 |
| T-AUTH-CTL-03 | `GET /api/v1/auth/me` | [依赖: T-AUTH-CTL-01] | 1h | 返回当前用户与权限 | 已完成 |
| T-AUTH-CTL-04 | `GET/POST/PUT /api/v1/users` | [依赖: T-AUTH-SVC-02] | 2h | 需 `system:user:manage` | 已完成 |
| T-AUTH-CTL-05 | `GET/PUT /api/v1/roles/{id}/permissions` | [依赖: T-AUTH-SVC-03] | 2h | 需 `system:role:manage` | 已完成 |
| T-AUTH-CTL-05a | `POST .../roles/{id}/permissions/restore-default` | [依赖: T-AUTH-SVC-03] | 1h | 恢复种子默认 | 已完成 |
| T-AUTH-CTL-06 | `PUT /api/v1/users/{id}/permissions` | [依赖: T-AUTH-SVC-03] | 1h | 需 `system:permission:grant` | 已完成 |
| T-AUTH-CTL-06a | `POST .../users/{id}/permissions/restore-default` | [依赖: T-AUTH-SVC-03] | 0.5h | 清空直授 | 已完成 |
| T-AUTH-CTL-07 | `PUT /api/v1/users/{id}/password` | [依赖: T-AUTH-SVC-02] | 1h | 管理员改密 | 已完成 |
| T-AUTH-CTL-08 | `DELETE /api/v1/users/{id}` | [依赖: T-AUTH-SVC-02] | 1h | 删用户+约束 | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-SVC-01 | Spring Security + JWT 过滤器链 | [依赖: T-INFRA-CTL-01] | 4h | 未带 token 访问业务接口 401 | 已完成 |
| T-AUTH-SVC-02 | `UserService`：BCrypt 校验、用户 CRUD | [依赖: T-INFRA-DB-01] | 3h | 密码不明文存储 | 已完成 |
| T-AUTH-SVC-03 | `PermissionService`：角色权限+用户直授合并 | [依赖: T-AUTH-SVC-02] | 3h | `hasAuthority` 与直授一致 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-MAP-01 | `SysUserMapper` + XML/注解 | [依赖: T-INFRA-DB-03] | 1h | 用户查询正常 | 已完成 |
| T-AUTH-MAP-02 | `SysRole/Permission/UserRole/UserPerm` Mapper | [依赖: T-INFRA-DB-03] | 2h | 关联查询权限列表 | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-REPO-01 | 实体类 `SysUser/SysRole/SysPermission` 等 + JavaDoc | [依赖: T-INFRA-DB-01] | 2h | 字段与表一致 | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-UT-FE-01 | 无 token 访问业务页跳转登录 | [依赖: T-AUTH-FE-03] | 0.5h | 路由守卫生效 | 已完成 |
| T-AUTH-UT-FE-02 | 前台角色不显示用户管理菜单 | [依赖: T-AUTH-FE-04] | 0.5h | 菜单按权限隐藏 | 已完成 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-IT-01 | 登录成功/密码错误 | [依赖: T-AUTH-CTL-01] | 1h | 200 vs 401 | 已完成 |
| T-AUTH-IT-02 | 无权限调用 `POST /users` 返回 403 | [依赖: T-AUTH-CTL-04] | 1h | 403 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUTH-EX-01 | 停用用户登录失败 | [依赖: T-AUTH-SVC-02] | 0.5h | 明确错误提示 | 已完成 |
| T-AUTH-EX-02 | 过期 JWT 访问业务接口 401 | [依赖: T-AUTH-SVC-01] | 0.5h | 401 | 已完成 |

**当前状态**：`已完成`

### 已知缺陷（后期修复）

| 编号 | 任务 | 说明 | 状态 |
|------|------|------|------|
| T-AUTH-BUG-01 | 用户管理权限：角色/直授后前端仍提示无权限 | 现象：授予 `system:user:manage` 后访问 `/system/users` 仍被路由守卫拦截；后端 `GET /api/v1/users` 集成测试可通过。已尝试路由前 `syncPermissions()`，**仍未解决**，保留后期修改 | **待开始** |

---

## 3. 客房管理（MOD-ROOM）

**模块目标**：房型/客房 CRUD、房态图、维修、前台置脏/保洁置空净、强制改房态。  
**Spec/plan**：spec §5；plan §3.3、API-ROOM

**用户可见内容**：房态图首页；房型/客房管理；维修、置脏/置净与强制改态操作。

**用户操作流程**：查看房态图筛选 → 管理员维护房型门市价 → 设置维修（原因+ETA）→ 前台「设为脏房」/ 保洁「设为空净」→ 店长强制改态（二次确认）。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-FE-01 | 房态图 `RoomBoard.vue`（色块+预抵/预离标签） | [依赖: T-AUTH-FE-03, T-ROOM-CTL-03] | 5h | 50 间渲染流畅 | 已完成 |
| T-ROOM-FE-02 | 房型管理页 CRUD | [依赖: T-ROOM-CTL-01] | 3h | 门市价可改 | 已完成 |
| T-ROOM-FE-03 | 客房管理页 CRUD | [依赖: T-ROOM-CTL-02] | 3h | 房号唯一校验提示 | 已完成 |
| T-ROOM-FE-04 | 维修对话框（原因、ETA 必填） | [依赖: T-ROOM-CTL-04] | 2h | 提交后房态维修 | 已完成 |
| T-ROOM-FE-05 | 强制改态对话框（原因二次确认） | [依赖: T-ROOM-CTL-06] | 2h | 无权限不显示入口 | 已完成 |
| T-ROOM-FE-06 | 房态图「设为脏房/设为空净」按钮 | [依赖: T-ROOM-CTL-07,08] | 1h | 按权限与当前态显示 | 已完成 |
| T-ROOM-FE-07 | 房态图指定日期查看（日期选择器） | [依赖: T-ROOM-CTL-03a] | 1h | 切换日期后预抵/预离标签随之变化 | 已完成 |
| T-ROOM-FE-08 | 楼层下拉独立数据源；展示态/库内态 | [依赖: T-ROOM-CTL-03b, T-ROOM-SVC-02a] | 1h | 筛选后仍可换楼层；过期预订不显示预订色 | 已完成 |
| T-ROOM-FE-09 | 房态图点击：订单列表+编辑+快速预订/入住 | [依赖: T-ROOM-CTL-03c, T-RES-FE-01, T-STAY-FE-01] | 5h | 查看日无占用可快速办理 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-CTL-01 | `CRUD /api/v1/room-types` | [依赖: T-AUTH-SVC-01] | 2h | 权限 `room:type:manage` | 已完成 |
| T-ROOM-CTL-02 | `CRUD /api/v1/rooms` | [依赖: T-ROOM-SVC-01] | 2h | 权限 `room:manage` | 已完成 |
| T-ROOM-CTL-03 | `GET /api/v1/rooms/board` | [依赖: T-ROOM-SVC-02] | 2h | 含 status+daily_tags | 已完成 |
| T-ROOM-CTL-03a | `GET /rooms/board?date=` 按指定日期算标签 | [依赖: T-ROOM-SVC-02] | 0.5h | 不传 date 等同当天 | 已完成 |
| T-ROOM-CTL-03b | `GET /rooms/floors` 全部楼层 | [依赖: T-ROOM-SVC-02] | 0.5h | 与楼层筛选无关 | 已完成 |
| T-ROOM-CTL-04 | `POST /rooms/{id}/maintenance` | [依赖: T-ROOM-SVC-03] | 1h | BR-11 校验 | 已完成 |
| T-ROOM-CTL-05 | `POST /rooms/{id}/maintenance/end` | [依赖: T-ROOM-SVC-03] | 1h | 结束维修 | 已完成 |
| T-ROOM-CTL-06 | `POST /rooms/{id}/status/force` | [依赖: T-ROOM-SVC-04] | 1h | 需 `room:status:force` | 已完成 |
| T-ROOM-CTL-07 | `POST /rooms/{id}/status/dirty` | [依赖: T-ROOM-SVC-04] | 1h | 需 `room:status:dirty` | 已完成 |
| T-ROOM-CTL-08 | `POST /rooms/{id}/status/clean` | [依赖: T-ROOM-SVC-04] | 1h | 需 `room:status:clean` | 已完成 |
| T-ROOM-CTL-03c | `GET /rooms/{id}/schedule` | [依赖: T-ROOM-SVC-02c] | 1h | 含 occupiedOnViewDate | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-SVC-01 | `RoomTypeService` / `RoomService` 基础 CRUD | [依赖: T-ROOM-MAP-01] | 3h | 房号唯一 | 已完成 |
| T-ROOM-SVC-02 | `RoomBoardService`：批量查房+算 daily_tags | [依赖: T-ROOM-SVC-01] | 4h | 无嵌套循环 | 已完成 |
| T-ROOM-SVC-02a | 房态图展示态按查看日+预订时刻 | [依赖: T-RES-DB-01, T-ROOM-MAP-03] | 2h | 查看日不在占用区间不展示预订 | 已完成 |
| T-ROOM-SVC-02b | 展示态：脏/空净不遮挡在住/预订；在住按日期区间 | [依赖: T-ROOM-SVC-02a, T-STAY-SVC-01] | 2h | 置脏后查看日仍显示预订/在住色 | 已完成 |
| T-RES-SVC-02b | 在住冲突按 18:00/12:00+缓冲；相邻 23-24/24-25 可售 | [依赖: T-RES-SVC-02] | 2h | Walk-in 传时刻；非全天重叠 | 已完成 |
| T-STAY-FE-04 | Walk-in 入住/离店时刻可编辑（默认 18:00/12:00） | [依赖: T-STAY-FE-01] | 1h | 与可售查询一致 | 已完成 |
| T-ROOM-SVC-03 | `RoomMaintenanceService`：维修开始/结束 | [依赖: T-ROOM-SVC-04] | 3h | 写 maintenance_log | 已完成 |
| T-ROOM-SVC-04 | `RoomStateMachine` 状态转换与 assert | [依赖: T-ROOM-SVC-01] | 4h | 非法转换抛 40001 | 已完成 |
| T-ROOM-SVC-02c | `RoomScheduleService` 客房日程（预订+在住） | [依赖: T-RES-SVC-01, T-STAY-SVC-01] | 3h | fromDate 起未来订单 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-MAP-01 | `RoomTypeMapper` / `RoomMapper` | [依赖: T-INFRA-DB-03] | 2h | 乐观锁 version 更新 | 已完成 |
| T-ROOM-MAP-02 | `RoomMaintenanceLogMapper` | [依赖: T-INFRA-DB-03] | 1h | 插入维修记录 | 已完成 |
| T-ROOM-MAP-03 | 房态图聚合查询 SQL（预订/在住 JOIN） | [依赖: T-ROOM-MAP-01] | 2h | 单次查询出 board 数据 | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-REPO-01 | 实体 `RoomType` / `Room` / `RoomMaintenanceLog` | [依赖: T-INFRA-DB-01] | 1h | JavaDoc 完整 | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-UT-FE-01 | 按楼层筛选房态图 | [依赖: T-ROOM-FE-01] | 0.5h | 筛选结果正确 | 已完成 |
| T-ROOM-UT-FE-03 | 切换查看日期刷新预抵/预离 | [依赖: T-ROOM-FE-07] | 0.5h | 与预订/在住日期一致 | 已完成 |
| T-ROOM-UT-FE-02 | 无强制权限不显示强制改态按钮 | [依赖: T-ROOM-FE-05] | 0.5h | 按钮隐藏 | 已完成 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-IT-01 | 房态图接口返回预抵标签 | [依赖: T-ROOM-CTL-03] | 1h | 有当日预订则带标签 | 已完成 |
| T-ROOM-IT-03 | `GET /board?date=yyyy-MM-dd` 正常返回 | [依赖: T-ROOM-CTL-03a] | 0.5h | code=0 | 已完成 |
| T-ROOM-IT-02 | 维修缺少原因返回 400 | [依赖: T-ROOM-CTL-04] | 0.5h | 校验失败 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-ROOM-EX-01 | 非法状态迁移被拒绝（40001） | [依赖: T-ROOM-SVC-04] | 1h | TC-12 对应 | 已完成 |
| T-ROOM-EX-02 | 乐观锁冲突返回 40901 | [依赖: T-ROOM-MAP-01] | 1h | 并发更新失败可刷新 | 已完成 |

**当前状态**：`已完成`

---

## 4. 预订管理（MOD-RES）

**模块目标**：手工预订、预排房、改期/取消、手动释放，禁止超售。  
**Spec/plan**：spec §6；BR-01/03/04；API-RES

**用户可见内容**：预订列表/表单；排房选择器；释放/No-show 确认框。

**用户操作流程**：新建预订 → 选房型/日期 → 预排房 → 到店前可修改/取消 → 未到店则手动释放。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-FE-01 | 预订列表页（筛选日期/状态） | [依赖: T-RES-CTL-02] | 3h | 分页列表正常 | 已完成 |
| T-RES-FE-02 | 新建/编辑预订表单 | [依赖: T-RES-CTL-01, T-RES-CTL-03] | 4h | 校验必填项 | 已完成 |
| T-RES-FE-03 | 预排房组件（调 availability API） | [依赖: T-RES-CTL-04, T-RES-CTL-07] | 3h | 不可售房不可选 | 已完成 |
| T-RES-FE-04 | 释放预订二次确认（可选 No-show） | [依赖: T-RES-CTL-06] | 2h | 释放后房态恢复 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-CTL-01 | `POST /api/v1/reservations` | [依赖: T-RES-SVC-01] | 1h | 生成 res_no | 已完成 |
| T-RES-CTL-02 | `GET /api/v1/reservations` | [依赖: T-RES-SVC-01] | 1h | 支持筛选 | 已完成 |
| T-RES-CTL-03 | `PUT /api/v1/reservations/{id}` | [依赖: T-RES-SVC-01] | 1h | 改期更新 | 已完成 |
| T-RES-CTL-04 | `POST /reservations/{id}/assign-room` | [依赖: T-RES-SVC-02] | 2h | 房态→RESERVED | 已完成 |
| T-RES-CTL-05 | `POST /reservations/{id}/cancel` | [依赖: T-RES-SVC-03] | 1h | 无罚金 | 已完成 |
| T-RES-CTL-06 | `POST /reservations/{id}/release` | [依赖: T-RES-SVC-03] | 1h | BR-03 | 已完成 |
| T-RES-CTL-07 | `GET /reservations/availability` | [依赖: T-RES-SVC-04] | 2h | 返回可排房列表 | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-SVC-01 | `ReservationService` 创建/查询/更新 | [依赖: T-RES-MAP-01] | 4h | 状态机正确 | 已完成 |
| T-RES-SVC-02 | `assignRoom`：可售校验+房态 RESERVED | [依赖: T-RES-SVC-04, T-ROOM-SVC-04] | 3h | BR-01 | 已完成 |
| T-RES-SVC-03 | `cancel/release`：释放房态+审计 | [依赖: T-RES-SVC-02, T-AUDIT-SVC-01] | 3h | BR-03/04；审计待 MOD-AUDIT | 已完成 |
| T-RES-SVC-04 | `RoomAvailabilityService.checkAssignable` | [依赖: T-ROOM-MAP-01] | 4h | 冲突返回 40002 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-MAP-01 | `ReservationMapper` CRUD + 日期冲突查询 | [依赖: T-INFRA-DB-03] | 2h | 参数化 SQL | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-REPO-01 | 实体 `Reservation` + 状态枚举 | [依赖: T-INFRA-DB-01] | 1h | 与 plan 一致 | 已完成 |
| T-RES-DB-01 | V7 `arrival_at`/`departure_at` 迁移与回填 | [依赖: T-INFRA-DB-01] | 1h | 默认 18:00/12:00 | 已完成 |
| T-RES-SVC-05 | `ReservationTimePolicy` 默认时刻与 1h 缓冲冲突 | [依赖: T-RES-DB-01] | 2h | BR-01/BR-12 | 已完成 |
| T-RES-FE-05 | 预订表单入住/离店时刻选择 | [依赖: T-RES-SVC-05] | 2h | 默认 18:00、12:00 | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-UT-FE-01 | 释放操作弹出确认框 | [依赖: T-RES-FE-04] | 0.5h | 取消则不释放 | 已完成 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-IT-01 | 预排房后房态 RESERVED | [依赖: T-RES-CTL-04] | 1h | TC-01 | 已完成 |
| T-RES-IT-02 | 超售场景返回 40002 | [依赖: T-RES-CTL-04] | 1h | TC-01 负例 | 已完成 |
| T-RES-IT-03 | 手动释放后房态 VACANT_CLEAN | [依赖: T-RES-CTL-06] | 1h | TC-09 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-RES-EX-01 | 对维修房预排房失败 | [依赖: T-RES-SVC-02] | 1h | 明确提示 | 已完成 |
| T-RES-EX-02 | 已入住预订不可重复入住 | [依赖: T-RES-SVC-01] | 0.5h | 状态校验 | 已完成 |

**当前状态**：`已完成`

---

## 5. 入住与在住（MOD-STAY）

**模块目标**：Walk-in、预订入住、在住列表、换房（整段重算触发点）。  
**Spec/plan**：spec §7；BR-02/06；API-STAY

**用户可见内容**：入住办理页；在住列表；换房对话框。

**用户操作流程**：Walk-in 选空净房→录入客人→入住；或预订查询→确认入住；在住详情→换房→选目标空净房。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-FE-01 | 入住页：Walk-in / 预订入住 Tab | [依赖: T-STAY-CTL-01, T-STAY-CTL-02] | 5h | 两入口可用 | 已完成 |
| T-STAY-FE-02 | 在住列表页 | [依赖: T-STAY-CTL-03] | 2h | 仅 IN_HOUSE | 已完成 |
| T-STAY-FE-03 | 换房对话框+目标房选择 | [依赖: T-STAY-CTL-04] | 3h | 成功后刷新账单 | 已完成 |
| T-STAY-FE-04 | 在住备注编辑 | [依赖: T-STAY-CTL-06] | 1h | 保存备注 | 已完成 |
| T-STAY-FE-05 | 在住列表：客人姓名查询 | [依赖: T-STAY-CTL-03] | 1h | 模糊过滤 | 已完成 |
| T-STAY-FE-06 | 在住列表：退款对话框 | [依赖: T-STAY-CTL-08] | 2h | 计费截止日+退款 | 已完成 |
| T-ROOM-FE-10 | 房态图：订单行退订退款/换房 | [依赖: T-STAY-CTL-08, T-RES-CTL-06a] | 3h | 预订/在住分支 | 已完成 |
| T-ROOM-FE-11 | 房态图：空净/脏房单按钮切换 | [依赖: T-ROOM-CTL-06/06a] | 1h | 脏↔净一键 | 已完成 |
| T-ROOM-FE-12 | 房态图：预订入住+在住退房（含结账） | [依赖: T-STAY-CTL-02, T-BILL-CTL-05] | 3h | 预订 CONFIRMED；Walk-in 须 payments | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-CTL-01 | `POST /api/v1/stays/walk-in` | [依赖: T-STAY-SVC-01] | 2h | 需开班（SHIFT） | 已完成 |
| T-STAY-CTL-02 | `POST /stays/check-in-from-reservation` | [依赖: T-STAY-SVC-02] | 2h | 预订→CHECKED_IN | 已完成 |
| T-STAY-CTL-03 | `GET /stays/in-house` | [依赖: T-STAY-SVC-01] | 1h | 列表 | 已完成 |
| T-STAY-CTL-04 | `POST /stays/{id}/change-room` | [依赖: T-STAY-SVC-03] | 2h | 触发重算 | 已完成 |
| T-STAY-CTL-06 | `PUT /stays/{id}/remark` | [依赖: T-STAY-SVC-01] | 1h | 备注 | 已完成 |
| T-STAY-CTL-07 | `GET /stays/in-house?guestName=` | [依赖: T-STAY-SVC-01] | 0.5h | 姓名模糊 | 已完成 |
| T-STAY-CTL-08 | `POST /stays/{id}/void-checkout` | [依赖: T-STAY-SVC-04] | 2h | 提前退房+退款 | 已完成 |
| T-RES-CTL-06a | `POST /reservations/{id}/cancel-with-refund` | [依赖: T-RES-SVC-03] | 1h | 退订+退款流水 | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-SVC-01 | `StayService.checkInWalkIn` | [依赖: T-ROOM-SVC-04, T-BILL-SVC-01, T-SHIFT-SVC-01] | 4h | 房态 OCCUPIED | 已完成 |
| T-STAY-SVC-02 | `checkInFromReservation` | [依赖: T-RES-SVC-01, T-STAY-SVC-01] | 3h | TC-02 | 已完成 |
| T-STAY-SVC-03 | `changeRoom`：原房脏、目标校验、调 Billing 重算 | [依赖: T-BILL-SVC-02, T-ROOM-SVC-04] | 4h | BR-06 | 已完成 |
| T-STAY-SVC-04 | `voidCheckout`：截断房费、退款流水、退房脏房 | [依赖: T-BILL-SVC-03] | 4h | §7.9 | 已完成 |
| T-BILL-SVC-03 | `truncateFolio` / `recordRefund` / `recordReservationRefund` | [依赖: T-BILL-MAP-02] | 3h | 支付流水 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-MAP-01 | `StayOrderMapper` / `StayGuestMapper` | [依赖: T-INFRA-DB-03] | 2h | 关联查询 | 已完成 |
| T-BILL-MAP-02 | `PaymentMapper` | [依赖: T-INFRA-DB-01] | 1h | 退款流水 | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-REPO-01 | 实体 `StayOrder` / `StayGuest` | [依赖: T-INFRA-DB-01] | 1h | JavaDoc 完整 | 已完成 |
| T-BILL-REPO-02 | 实体 `Payment` + V11 可空 folio_id | [依赖: T-INFRA-DB-01] | 1h | 预订退款 | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-UT-FE-01 | 脏房不可选为 Walk-in 目标 | [依赖: T-STAY-FE-01] | 0.5h | 列表过滤 | 已完成 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-IT-01 | Walk-in 成功 | [依赖: T-STAY-CTL-01] | 1h | TC-03 | 已完成 |
| T-STAY-IT-02 | 预订入住成功 | [依赖: T-STAY-CTL-02] | 1h | TC-02 | 已完成 |
| T-STAY-IT-03 | 换房后 folio 金额变化 | [依赖: T-STAY-CTL-04] | 1h | TC-04 | 待开始 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAY-EX-01 | 非空净房入住拒绝 40001 | [依赖: T-STAY-SVC-01] | 1h | TC-11 | 待开始 |
| T-STAY-EX-02 | 重复入住同一房拦截 | [依赖: T-STAY-SVC-01] | 1h | 幂等/校验 | 已完成 |
| T-STAY-EX-03 | 未开班 Walk-in 失败 40003 | [依赖: T-SHIFT-SVC-01] | 0.5h | OQ-03 | 已完成 |

**当前状态**：`已完成`（退房结账见 MOD-BILL；T-STAY-IT-03 换房金额断言待补）

---

## 6. 账单与退房（MOD-BILL）

**模块目标**：按晚计价、改价、分笔支付、退房结清。  
**Spec/plan**：spec §8；BR-05/06/07；API-BILL

**用户可见内容**：账单明细；改价入口（有权限）；退房收银台（多支付方式）。

**用户操作流程**：入住时录入支付结清 →（授权）改价 → 退房仅释放客房 → 房态脏房 + `hk_task`。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-FE-01 | 账单展示组件（明细行） | [依赖: T-BILL-CTL-01] | 2h | 显示晚数与金额 | 已完成 |
| T-BILL-FE-02 | 改价对话框（无权限隐藏） | [依赖: T-BILL-CTL-03] | 2h | TC-05/06 | 已完成 |
| T-BILL-FE-03 | 入住页结账+在住退房按钮 | [依赖: T-BILL-CTL-04, T-BILL-CTL-05] | 4h | 入住收齐；退房仅释放 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-CTL-01 | `GET /folios/by-stay/{stayId}` | [依赖: T-BILL-SVC-01] | 1h | 返回行项目 | 已完成 |
| T-BILL-CTL-02 | `POST /folios/{id}/recalculate` | [依赖: T-BILL-SVC-02] | 1h | 重算 | 已完成 |
| T-BILL-CTL-03 | `POST /folios/{id}/adjust-price` | [依赖: T-BILL-SVC-03] | 1h | `billing:price:adjust` | 已完成 |
| T-BILL-CTL-04 | `POST /folios/{id}/payments` | [依赖: T-BILL-SVC-04] | 2h | 关联 shift_id | 已完成 |
| T-BILL-CTL-05 | `POST /stays/{id}/checkout`（仅释放房） | [依赖: T-BILL-SVC-05] | 2h | 入住已结账 | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-SVC-01 | `BillingService.initFolioLines`+`settleFolioAtCheckIn` | [依赖: T-BILL-MAP-01] | 4h | 入住须结清 | 已完成 |
| T-BILL-SVC-02 | `recalculateFullStay` 换房整段重算 | [依赖: T-BILL-SVC-01] | 3h | BR-06 | 已完成 |
| T-BILL-SVC-03 | `adjustPrice` + 审计 | [依赖: T-AUDIT-SVC-01] | 2h | BR-05 | 已完成（审计待 MOD-AUDIT） |
| T-BILL-SVC-04 | `PaymentService.addPayment` | [依赖: T-SHIFT-SVC-01] | 2h | 累计 paid | 已完成 |
| T-BILL-SVC-05 | `CheckoutService.checkout` | [依赖: T-BILL-SVC-04, T-ROOM-SVC-04, T-HK-SVC-01] | 4h | 结清+脏房+hk_task | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-MAP-01 | `FolioMapper` / `FolioLineMapper` / `PaymentMapper` | [依赖: T-INFRA-DB-03] | 2h | 参数化 | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-REPO-01 | 实体 `Folio` / `FolioLine` / `Payment` | [依赖: T-INFRA-DB-01] | 1h | 无押金字段 | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-UT-FE-01 | 支付合计不足时禁用退房按钮 | [依赖: T-BILL-FE-03] | 0.5h | UI 校验 | 待开始 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-IT-01 | 无改价权限 403 | [依赖: T-BILL-CTL-03] | 0.5h | TC-05 | 待开始 |
| T-BILL-IT-02 | 授权改价成功且有审计 | [依赖: T-BILL-CTL-03] | 1h | TC-06 | 待开始 |
| T-BILL-IT-03 | 入住结清后退房释放+hk_task | [依赖: T-BILL-CTL-05] | 1h | TC-07 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-BILL-EX-01 | 入住收款不足 40004 | [依赖: T-BILL-SVC-01] | 0.5h | BR-07 | 已完成 |
| T-BILL-EX-02 | 未开班收款 40003 | [依赖: T-BILL-SVC-04] | 0.5h | OQ-03 | 待开始 |

**当前状态**：`已完成`（首批；改价审计、TC-05/06 用例待 MOD-AUDIT 与补充 IT）

---

## 7. 保洁管理（MOD-HK）

**模块目标**：脏房生成保洁任务，完成后自动空净。  
**Spec/plan**：spec §9；BR-10；API-HK

**用户可见内容**：待打扫列表（保洁角色）；完成按钮。

**用户操作流程**：保洁登录 → 查看待扫 → 完成打扫 → 房态空净。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-FE-01 | 保洁任务列表页（按楼层筛） | [依赖: T-HK-CTL-01] | 2h | 仅 PENDING | 已完成 |
| T-HK-FE-02 | 完成打扫确认 | [依赖: T-HK-CTL-02] | 1h | 二次确认 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-CTL-01 | `GET /api/v1/hk/tasks` | [依赖: T-HK-SVC-01] | 1h | `hk:view` | 已完成 |
| T-HK-CTL-02 | `POST /hk/tasks/{id}/complete` | [依赖: T-HK-SVC-02] | 1h | `hk:complete` | 已完成 |
| T-HK-CTL-03 | `POST /rooms/{id}/mark-dirty` | [依赖: T-HK-SVC-01] | 1h | 前台置脏 | 待开始 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-SVC-01 | `HousekeepingService.createTaskOnDirty` | [依赖: T-HK-MAP-01] | 2h | 脏房必有任务 | 已完成 |
| T-HK-SVC-02 | `completeTask`→房态 VACANT_CLEAN | [依赖: T-ROOM-SVC-04] | 2h | BR-10 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-MAP-01 | `HkTaskMapper` | [依赖: T-INFRA-DB-03] | 1h | CRUD | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-REPO-01 | 实体 `HkTask` | [依赖: T-INFRA-DB-01] | 0.5h | — | 已完成 |

**当前状态**：`已完成`

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-UT-FE-01 | 保洁角色仅见保洁菜单 | [依赖: T-HK-FE-01] | 0.5h | 权限隔离 | 待开始 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-IT-01 | 退房后生成 hk_task | [依赖: T-BILL-CTL-05] | 1h | TC-07 | 待开始 |
| T-HK-IT-02 | 完成后房态空净 | [依赖: T-HK-CTL-02] | 1h | TC-08 | 待开始 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-HK-EX-01 | 重复完成同一任务幂等处理 | [依赖: T-HK-SVC-02] | 0.5h | 不报错或 409 | 待开始 |

**当前状态**：`已完成`（列表/完成 API；T-HK-IT/EX 部分待补）

---

## 8. 交班管理（MOD-SHIFT）

**模块目标**：开班/结班、本班收款汇总、待办交接。  
**Spec/plan**：spec §11.1；OQ-03；API-SHIFT

**用户可见内容**：开班/结班页；交班预览与结班单。

**用户操作流程**：上班开班 → 业务收款关联当前班 → 结班预览待办与收款 → 确认结班（有待办则阻断或经理强制）。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-FE-01 | 开班/当前班状态展示 | [依赖: T-SHIFT-CTL-01, T-SHIFT-CTL-02] | 2h | 未开班明显提示 | 已完成 |
| T-SHIFT-FE-02 | 结班预览+确认（待办列表） | [依赖: T-SHIFT-CTL-03, T-SHIFT-CTL-04] | 3h | 有待办阻断或强制 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-CTL-01 | `POST /api/v1/shifts/open` | [依赖: T-SHIFT-SVC-01] | 1h | 创建 OPEN session | 已完成 |
| T-SHIFT-CTL-02 | `GET /shifts/current` | [依赖: T-SHIFT-SVC-01] | 1h | 当前操作员班 | 已完成 |
| T-SHIFT-CTL-03 | `GET /shifts/{id}/handover-preview` | [依赖: T-SHIFT-SVC-02] | 2h | 收款+待办 | 已完成 |
| T-SHIFT-CTL-04 | `POST /shifts/{id}/close` | [依赖: T-SHIFT-SVC-03] | 2h | 生成 handover | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-SVC-01 | `ShiftSessionService.open/getCurrent` | [依赖: T-SHIFT-MAP-01] | 3h | 一人一 open 班 | 已完成 |
| T-SHIFT-SVC-02 | `buildHandoverPreview`：聚合 payment+待办 | [依赖: T-BILL-MAP-01] | 4h | TC-10 数据正确 | 已完成 |
| T-SHIFT-SVC-03 | `closeShift`：待办阻断/force_close | [依赖: T-SHIFT-SVC-02] | 3h | OQ-03 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-MAP-01 | `ShiftSessionMapper` / `ShiftHandoverMapper` | [依赖: T-INFRA-DB-03] | 2h | 快照 JSON | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-REPO-01 | 实体 `ShiftSession` / `ShiftHandover` | [依赖: T-INFRA-DB-01] | 1h | — | 已完成 |

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-UT-FE-01 | 有待办时结班按钮禁用（无 force 权） | [依赖: T-SHIFT-FE-02] | 0.5h | UI 阻断 | 待开始 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-IT-01 | 结班单含支付方式汇总 | [依赖: T-SHIFT-CTL-04] | 1h | TC-10 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-SHIFT-EX-01 | 重复开班处理 | [依赖: T-SHIFT-SVC-01] | 0.5h | 返回已有班或 409 | 待开始 |

**当前状态**：`已完成`（首批；T-SHIFT-EX-01 重复结班已覆盖 IT）

---

## 9. 轻量统计（MOD-STAT）

**模块目标**：出租率、房费营收简单汇总（非独立看板）。  
**Spec/plan**：spec §11.2；API-STAT

**用户可见内容**：统计页两张卡片/简单表格。

**用户操作流程**：店长/管理员选择日期区间 → 查看出租率与营收。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-FE-01 | 统计页：出租率+营收 | [依赖: T-STAT-CTL-01, T-STAT-CTL-02] | 2h | 需 `stat:view` | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-CTL-01 | `GET /api/v1/stats/occupancy` | [依赖: T-STAT-SVC-01] | 1h | 在住/可售 | 已完成 |
| T-STAT-CTL-02 | `GET /api/v1/stats/revenue` | [依赖: T-STAT-SVC-01] | 1h | 区间房费合计 | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-SVC-01 | `StatsService` 聚合查询 | [依赖: T-STAY-MAP-01, T-BILL-MAP-01] | 3h | 单店全局 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-MAP-01 | 统计 SQL（出租率/营收） | [依赖: T-INFRA-DB-03] | 2h | 参数化日期 | 已完成 |

### Repository/数据持久化任务

无新增表。

### 页面测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-UT-FE-01 | 前台角色不可见统计页 | [依赖: T-STAT-FE-01] | 0.5h | 路由守卫 | 待开始 |

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-IT-01 | 营收与已结账 payment 一致 | [依赖: T-STAT-CTL-02] | 1h | 数据一致 | 已完成 |

### 异常情况测试

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-STAT-EX-01 | 日期区间非法校验 | [依赖: T-STAT-CTL-02] | 0.5h | 400 | 已完成 |

**当前状态**：`已完成`（首批；T-STAT-UT-FE-01 路由守卫待补）

---

## 10. 操作审计（MOD-AUDIT）

**模块目标**：订单全生命周期操作日志，改价含前后值。  
**Spec/plan**：spec §10.3；plan §8.1

**用户可见内容**：审计日志查询页（管理员/店长）。

**用户操作流程**：按时间/业务类型筛选 → 查看详情 JSON。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-FE-01 | 审计日志列表页 | [依赖: T-AUDIT-CTL-01] | 2h | 分页筛选 | 已完成 |

### Controller 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-CTL-01 | `GET /api/v1/audit/logs` | [依赖: T-AUDIT-SVC-01] | 1h | `audit:view` | 已完成 |

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-SVC-01 | `OperationLogService.save/query` | [依赖: T-AUDIT-MAP-01] | 2h | 分页查询 | 已完成 |
| T-AUDIT-SVC-02 | `OperationLogAspect` 切面接入各模块 | [依赖: T-AUDIT-SVC-01] | 4h | 预订/入住/改价/换房/退房/释放/强改房态 | 已完成 |

### Mapper 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-MAP-01 | `OperationLogMapper` | [依赖: T-INFRA-DB-03] | 1h | 插入+条件查询 | 已完成 |

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-REPO-01 | 实体 `OperationLog` | [依赖: T-INFRA-DB-01] | 0.5h | — | 已完成 |

### 页面测试方法

无独立页面测试（合并 T-AUDIT-FE-01）。

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-AUDIT-IT-01 | 改价后 audit 表有 before/after | [依赖: T-BILL-CTL-03] | 1h | TC-06 | 已完成 |

### 异常情况测试

无。

**当前状态**：`已完成`（首批）

---

## 11. 集成测试与验收（MOD-QA）

**模块目标**：覆盖 spec §18 与 plan TC-01～12 全流程。  
**Spec/plan**：spec §18；plan §10

**用户可见内容**：无（自动化/手工测试）。

**用户操作流程**：按验收用例脚本执行。

### 前端页面任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-QA-FE-01 | （可选）Playwright E2E：登录→入住→退房 | [依赖: T-BILL-FE-03] | 6h | 脚本通过 | 待开始 |

### Controller 层任务

无（使用已存在 API）。

### Service 层任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-QA-SVC-01 | `BillingService` 单元测试：晚数计算 | [依赖: T-BILL-SVC-01] | 2h | 边界日期覆盖 | 待开始 |
| T-QA-SVC-02 | `RoomStateMachine` 单元测试 | [依赖: T-ROOM-SVC-04] | 2h | 合法/非法迁移 | 待开始 |

### Mapper 层任务

无。

### Repository/数据持久化任务

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-QA-DB-01 | 测试环境 H2/MySQL test 数据回滚策略 | [依赖: T-INFRA-DB-01] | 2h | `@SpringBootTest` 可重复跑 | 待开始 |

### 页面测试方法

见 T-QA-FE-01。

### 接口测试方法

| 编号 | 任务 | 依赖 | 工时 | 验收标准 | 状态 |
|------|------|------|------|----------|------|
| T-QA-IT-01 | 全流程集成测试 TC-01～10 | [依赖: T-SHIFT-CTL-04] | 6h | 全部通过 | 待开始 |
| T-QA-IT-02 | 异常用例 TC-11/12 | [依赖: T-STAY-CTL-01] | 2h | 拒绝符合 BR | 待开始 |

### 异常情况测试

已含于 T-QA-IT-02。

**当前状态**：`待开始`

---

## 附录：推荐执行顺序

```
T-INFRA-* → T-AUTH-* → T-ROOM-* → T-RES-* → T-STAY-* + T-BILL-*（已完成首批）
→ T-HK-*（已完成）→ T-SHIFT-*（已完成）→ T-STAT-*（已完成）→ T-AUDIT-*（已完成）→ T-QA-*
```

**下一执行**：`T-QA`（见 §11）全流程与异常用例。

**总工时估算（MVP）**：约 **185h**（含测试，不含可选 E2E）
