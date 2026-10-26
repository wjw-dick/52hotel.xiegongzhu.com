# 云宿 SaaS · 民宿/酒店数字化运营平台（多端）

> 一套面向民宿、单体酒店与小型连锁的 SaaS 运营平台：平台方统一开通租户，租户在 PC 后台完成门店、房型、房价、订单、入住、房务、结算、会员与报表管理，C 端通过 H5 / 微信小程序 / Flutter App 完成查房、预订、订单查询与会员操作。

- 交付基线：**v1.0.0（2026-10-26）**
- 部署形态：**Nginx 反向代理 + 宝塔/裸机 systemd 直接部署**（生产首选）；仓库内另附 Dockerfile 作为可选辅助方案，本次不构建镜像。
- 实现口径：核心业务闭环已实现并通过接口联调；微信开放平台、支付、公安报送、OTA、门锁等外部能力当前为**演示/模拟实现**，接入真实服务需补充资质、密钥与回调地址。详见「[实现状态与边界](#实现状态与边界)」。

---

## 1. 产品矩阵与端清单

| 端 | 面向用户 | 技术形态 | 访问方式 | 当前状态 |
|---|---|---|---|---|
| 平台端 | system 超管 / 平台运营 | Vue3 + Element Plus | `/admin/`（平台账号登录） | 已实现：租户开通/停用/续期/配额、平台看板、全局字典、平台审计、下钻只读 |
| 租户 PC 后台 | 租户管理员、店长、前台、保洁、财务、OTA 运营 | Vue3 + Element Plus | `/admin/`（租户账号登录） | 已实现：主数据、预订、前台、房务、财务、会员、报表、配置、用户、审计 |
| 前台 / 客房管家端 | 前台、店长 | 同一 PC 后台的「前台接待」模块（按角色授权） | `/admin/front-desk` | 已实现：工作台、入住/退房、续住/换房、押金、实名登记、交接班入口 |
| 保洁 / 客房服务端 | 保洁、客房主管 | 同一 PC 后台的「房务保洁」模块（按角色授权） | `/admin/housekeeping` | 已实现：清扫派单、房态上报、报修、布草、客诉 |
| H5 门户 | C 端住客 | Vue3 + Vite，移动优先 | `/` | 已实现：首页、房型详情、预订、订单查询、会员中心 |
| 微信小程序 | C 端住客 | 微信原生小程序 | 微信开发者工具导入 `miniprogram/` | 页面与接口已实现；需开发者工具/真机验证，登录为 demo 模式 |
| 独立 App | C 端住客 | Flutter（iOS / Android） | `app/` | 工程骨架与首页/预订/订单/会员页面已提供；本机未安装 Flutter SDK，未做编译验证 |
| 公众号（服务号） | C 端粉丝 | 微信服务号 + H5 | 微信公众平台 | 后台已提供微信配置/模板配置/站内消息；关注回复、菜单同步、模板真实下发等需外部服务号资质与联调 |

---

## 2. 目录结构

```text
52hotel.xiegongzhu.com/
├── backend/                     # Spring Boot 3 后端
│   ├── src/main/java/com/hotel/saas/
│   │   ├── auth/                # 登录、JWT、微信 demo 登录、改密
│   │   ├── booking/             # 订单、预订、入住、退房、换房、续住、公安报送
│   │   ├── common/              # 统一响应、异常、租户上下文、拦截器、限流、加密
│   │   ├── config/              # MyBatis-Plus 租户插件、演示数据、Web 配置
│   │   ├── configcenter/        # 配置、字典、菜单、用户、存储、微信、消息
│   │   ├── finance/             # 结算、收银、押金、挂账、交接班
│   │   ├── housekeeping/        # 清扫、报修、布草、客诉
│   │   ├── master/              # 门店、房型、房间、房价、渠道/餐饮/门锁
│   │   ├── member/              # 会员、积分、优惠券、C 端会员中心、公开门户
│   │   ├── platform/            # 租户管理、平台看板、到期治理
│   │   └── report/              # 经营看板、报表、CSV 导出
│   └── src/main/resources/application.yml
├── admin-web/                   # PC 管理后台（base=/admin/）
├── portal-web/                  # H5 公开门户 + 预订 + 会员（base=/）
├── miniprogram/                 # 微信原生小程序
├── app/                         # Flutter App 工程骨架
├── sql/init.sql                 # MySQL 8 建表脚本（幂等）
├── scripts/start.sh             # 一键检查/构建/启动
├── scripts/stop.sh              # 停止一键启动的进程
├── scripts/test_api.sh          # 演示环境 API 冒烟（会写测试数据）
├── start.sh / stop.sh           # 根目录快捷入口
├── nginx/hotel-saas.conf        # 生产 Nginx 站点配置
├── nginx/nginx.conf             # Docker 可选方案使用的完整 Nginx 配置
├── docker/supervisord.conf      # Docker 可选方案的进程托管
├── Dockerfile                   # 可选辅助部署定义（本次不构建镜像）
├── docs/                        # 项目文档与各端用户手册
├── PRO.md                       # 产品需求说明（权威）
├── tasks.md                     # 开发任务清单 T01~T75
├── checklist.md                 # 上线验收清单
└── CLAUDE.md                    # 交付协作约束（用户提供版本）
```

---

## 3. 技术栈

| 层级 | 选型 | 说明 |
|---|---|---|
| 后端 | Java 17 + Spring Boot 3.2.5 | Web / Validation / Security / AOP |
| ORM | MyBatis-Plus 3.5.7 | `TenantLineInnerInterceptor` 行级租户隔离、逻辑删除、分页 |
| 数据库 | MySQL 8.0 | `utf8mb4 / utf8mb4_unicode_ci`，HikariCP 最大连接 20 |
| 缓存 | Redis 7 | 登录会话辅助、限流、防重、编号锁；不可用时可按配置降级 |
| 消息 | RabbitMQ（依赖已引入） | 当前业务通知以站内消息落库为主，异步 MQ 消费为预留能力 |
| 鉴权 | JWT（jjwt）+ Spring Security PasswordEncoder | 无状态 Bearer Token；平台/租户/会员三类主体 |
| 后台前端 | Vue 3.5 + Vite 5 + TypeScript + Element Plus + Pinia + ECharts | 响应式 PC 管理端，按角色过滤菜单与路由 |
| H5 | Vue 3.5 + Vite 5 + TypeScript + vue-router | 独立门户，移动优先，支持中英切换 |
| 小程序 | 微信原生小程序 | 首页/预订/订单/会员/房型详情 |
| App | Flutter 3（Dart ≥ 3.3） | 预订客户端骨架，账号体系预留微信/手机号 |
| 对象存储 | 本地磁盘 / MinIO（S3 兼容） | 后台可切换驱动；OSS/COS 通过 MinIO 兼容层或后续适配 |
| 反向代理 | Nginx | 静态资源 + `/api` + `/uploads` 反向代理 |
| 部署 | 宝塔 / 裸机 systemd | Dockerfile 仅作可选辅助，不作为主方案 |

---

## 4. 快速开始

### 4.1 环境要求

- JDK 17+、Maven 3.8+
- Node.js 18+、npm
- MySQL 8.0（字符集 utf8mb4）
- Redis 6/7（可设密码；未启动时需 `ALLOW_DEGRADED=true` 才允许降级）
- 可选：Nginx、微信开发者工具、Flutter SDK

### 4.2 一键启动（推荐用于开发/验收）

```bash
cd /home/ai/52hotel.xiegongzhu.com
cp .env.example .env     # 首次使用；按实际数据库/Redis 修改
./start.sh
```

脚本会依次完成：

1. 加载 `.env`，检查 JDK 17+、Maven、Node 18+、npm、MySQL 客户端；
2. 检查 MySQL 连接，必要时用 `DB_ROOT_PASSWORD` 建库建用户，并执行 `sql/init.sql`；
3. 检查 Redis 连通性（失败时依据 `ALLOW_DEGRADED` 决定退出或降级）；
4. 源码比 jar 新时自动 `mvn package`，然后启动后端；
5. 安装前端依赖（缺少 `node_modules` 时）并启动 PC 后台与 H5；
6. 打印访问地址、演示账号与日志位置。

停止：

```bash
./stop.sh
```

> 脚本只管理自己拉起的后端/前端进程，不会停止系统 MySQL、Redis、Nginx。

### 4.3 本机演示地址（当前 `.env`）

| 项目 | 地址 |
|---|---|
| 后端健康检查 | <http://127.0.0.1:19090/api/health> |
| PC 管理后台 | <http://127.0.0.1:5173/admin/> |
| H5 门户 | <http://127.0.0.1:5174/> |
| 小程序 API 基址 | `http://<局域网IP>:19090/api`（在 `miniprogram/app.js` 修改） |

> 本机 `8080` 被其他服务占用，因此演示环境将 `SERVER_PORT` 设为 `19090`。生产环境保持默认 `8080` 即可，Nginx 配置按实际端口调整。

### 4.4 演示账号

| 主体 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 平台超管 | `admin` | `admin123` | 登录时不填租户编码 |
| 租户管理员（杭州） | `admin` | `admin123` | 租户编码 `YQ-HZ` |
| 租户管理员（上海） | `admin` | `admin123` | 租户编码 `XH-SH` |
| 前台 | `front` | `front123` | 租户编码 `YQ-HZ` |
| 保洁 | `house` | `house123` | 租户编码 `YQ-HZ` |
| 财务 | `finance` | `finance123` | 租户编码 `YQ-HZ` |

> 演示账号由 `DataInitializer` 在 `SEED_DEMO=true` 时自动创建；生产环境请设置 `SEED_DEMO=false` 并立即修改所有默认密码。

### 4.5 手动启动

```bash
# 1) 初始化数据库
mysql -h 127.0.0.1 -P 3306 -u root -p < sql/init.sql

# 2) 构建并启动后端
cd backend
mvn -DskipTests package
SERVER_PORT=8080 \
DB_URL='jdbc:mysql://127.0.0.1:3306/hotel_saas?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' \
DB_USERNAME=hotel DB_PASSWORD='Hotel@2026' \
REDIS_HOST=127.0.0.1 REDIS_PORT=6379 REDIS_PASSWORD='' \
java -jar target/hotel-saas.jar

# 3) 启动 PC 后台（另开终端）
cd admin-web && npm install && npm run dev

# 4) 启动 H5 门户（另开终端）
cd portal-web && npm install && npm run dev
```

Vite 通过 `VITE_API_TARGET` 代理 `/api`，默认 `http://127.0.0.1:8080`；演示环境请在启动前设置 `VITE_API_TARGET=http://127.0.0.1:19090`。

### 4.6 生产构建

```bash
cd admin-web && npm ci && npm run build     # 产物 admin-web/dist
cd ../portal-web && npm ci && npm run build # 产物 portal-web/dist
cd ../backend && mvn -DskipTests package    # 产物 backend/target/hotel-saas.jar
```

前端产物由 Nginx 直接托管，后端由 systemd 托管，详见 [docs/部署与运维.md](docs/部署与运维.md)。

---

## 5. 关键配置

完整变量见 `.env.example`，常用项：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8080` | 后端端口 |
| `ADMIN_PORT` / `PORTAL_PORT` | `5173` / `5174` | 开发态前端端口 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `127.0.0.1` / `3306` / `hotel_saas` | MySQL 连接 |
| `DB_USERNAME` / `DB_PASSWORD` | `hotel` / `Hotel@2026` | 业务库账号（生产必须更换） |
| `DB_AUTO_INIT` | `true` | 启动时执行幂等建表脚本 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `127.0.0.1` / `6379` / 空 | Redis 连接 |
| `ALLOW_DEGRADED` | `false` | Redis 不可用时是否允许降级 |
| `JWT_SECRET` | 示例值 | **生产必须更换为 ≥32 字节随机串** |
| `JWT_EXPIRE_HOURS` | `12` | 登录有效期 |
| `UPLOAD_DIR` | `./data/uploads` | 本地存储目录 |
| `PUBLIC_BASE_URL` | `http://127.0.0.1:8080` | 上传文件对外基址 |
| `SEED_DEMO` | `true` | 是否写入演示租户/账号/数据 |

---

## 6. 实现状态与边界

### 6.1 已实现并联调通过

- SaaS 租户隔离：MyBatis-Plus 自动注入 `tenant_id`；平台表白名单；跨租户访问 403 并写审计。
- 平台端：租户开通/停用/续期/配额、平台看板、全局字典、平台审计、按 `X-Tenant-Id` 下钻只读。
- 租户端：门店/楼栋/房型/房间、房价日历与改价记录、房态与变更日志、CSV 导入导出。
- 预订闭环：公开下单、后台下单、可用房校验、防重、取消、入住、押金、换房、续住、退房、结算、房态释放、自动生成清扫任务。
- 前台/房务/财务/会员：实名登记（模拟报送）、续住换房记录、清扫/报修/布草/客诉、结算/收银/押金/挂账/交接班、会员等级/积分/优惠券。
- 报表：经营概览、租户报表（入住率/ADR/RevPAR/渠道/房型）、CSV 导出。
- 配置中心：平台参数、租户配置、多语言词条、菜单配置、对象存储（本地/MinIO）、微信配置（密钥加密掩码）、消息模板、站内消息。
- 账号安全：平台/租户/会员修改密码、登录失败限流、操作审计、角色模块授权、平台下钻只读。
- 端：PC 后台与 H5 均通过生产构建；小程序页面/脚本通过静态语法检查；接口集成测试 92 项通过。

### 6.2 演示/模拟或待外部联调

| 能力 | 当前实现 | 上线前需要 |
|---|---|---|
| 微信登录 / UnionID | `openId` 直传的 demo 登录 | 小程序/公众号/App 资质、`code2Session`、开放平台绑定、合法域名 |
| 微信支付 / 退款 | 仅记录 `payType` 与结算金额 | 商户号、证书、支付回调、对账单 |
| 公安实名报送 | 入住时写模拟成功记录，可重试 | 当地公安旅馆业系统接口、密钥、网络准入 |
| OTA 渠道同步 | 渠道列表 + 手动同步时间（模拟） | 携程/美团/飞猪等开放平台授权与回调 |
| 智能门锁 / 身份证阅读器 / 自助机 | 远程开门/发卡为模拟指令 | 厂商 SDK/网关、设备联调 |
| 公众号模板消息 / 小程序订阅消息 | 模板配置、站内消息落库 | 模板 ID、认证服务号、发送接口与重试队列 |
| 短信 | 预留开关与字段 | 短信服务商账号、签名模板 |
| 对象存储 OSS/COS/S3 | 本地 + MinIO 已实现 | 其他驱动适配或使用 S3 兼容网关 |
| PDF 打印 | CSV 导出已实现 | 引入 PDF 引擎与打印模板（未实现） |
| 备份/监控 | 文档给出方案 | 生产环境 cron/宝塔计划任务、监控告警接入 |
| Flutter App | 工程骨架与主要页面 | 安装 Flutter SDK，`flutter create .` 补齐平台目录后真机验证 |
| 微信小程序 | 页面与请求封装完成 | 微信开发者工具真机验证、合法域名与类目审核 |

---

## 7. 验证记录

| 验证项 | 结果 | 证据 |
|---|---|---|
| 后端编译打包 | 通过 | `backend/target/hotel-saas.jar` |
| 后端健康检查 | 通过 | `GET /api/health` → `code=0, status=UP` |
| 接口集成测试 | **92 通过 / 0 失败** | `scripts/test_api.sh`，覆盖公开门户、下单防重、订单全流程、前台、房务、财务、会员、平台、隔离 |
| 角色权限 | 通过 | 前台可订单不可财务；保洁可房务不可订单；财务可财务不可房务；会员不可进后台；平台下钻写操作 403 |
| PC 后台生产构建 | 通过 | `npm run build` |
| H5 生产构建 | 通过 | `npm run build` |
| 一键启动 / 停止 | 通过 | `./start.sh`、`./stop.sh` |
| 小程序脚本语法 | 通过 | 全部 `.js` 执行 `node --check` |
| Flutter 编译 | 未验证 | 本机无 Flutter SDK |
| 微信真机 / 外部接口 | 未验证 | 缺外部资质与联调环境 |

完整证据与验收口径见 [docs/验收报告.md](docs/验收报告.md)。

---

## 8. 文档索引

| 文档 | 内容 |
|---|---|
| [docs/架构设计.md](docs/架构设计.md) | 总体架构、模块划分、技术栈、部署拓扑、关键业务时序 |
| [docs/数据库设计.md](docs/数据库设计.md) | 表清单、关系、租户隔离、索引、字符集、备份策略 |
| [docs/API接口.md](docs/API接口.md) | 统一响应、鉴权、按模块接口清单、错误码 |
| [docs/部署与运维.md](docs/部署与运维.md) | Nginx + systemd 部署、SSL、日志、备份、监控、排障、Docker 可选说明 |
| [docs/SaaS隔离与安全.md](docs/SaaS隔离与安全.md) | 租户插件、白名单、租户上下文、权限拦截、审计与安全基线 |
| [docs/后台配置说明.md](docs/后台配置说明.md) | 租户/权限/存储/微信/多语言/菜单配置操作 |
| [docs/业务差异说明.md](docs/业务差异说明.md) | 相对焊工考试系统的住宿业务域差异 |
| [docs/验收报告.md](docs/验收报告.md) | 验收结果、证据、未闭环事项 |
| [docs/用户手册/](docs/用户手册/) | PC 平台端、租户后台、前台、保洁、H5、小程序、App、公众号分册 |

---

## 9. 交付清单

- [x] 后端源码（Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT）
- [x] PC 管理后台源码（Vue3 + Element Plus）
- [x] H5 门户源码（Vue3 + Vite）
- [x] 微信小程序源码
- [x] Flutter App 工程骨架
- [x] 数据库初始化脚本 `sql/init.sql`
- [x] 一键启动/停止脚本、`.env.example`
- [x] Nginx 配置、systemd 部署文档
- [x] Dockerfile（可选辅助，未构建镜像）
- [x] README、架构、数据库、API、部署运维、SaaS 隔离、配置、差异、验收文档
- [x] 各端用户使用说明书
- [x] `CLAUDE.md`（用户提供版本，原样保留）

---

## 10. 版本与维护约定

| 版本 | 日期 | 内容 |
|---|---|---|
| 1.0.0 | 2026-10-26 | 首版：SaaS 底座、住宿业务闭环、多端、文档与一键部署脚本 |

- 代码变更遵循 `CLAUDE.md`：中文沟通、外科手术式修改、不做无关重构、不执行破坏性命令。
- 数据库变更必须同步更新 `sql/init.sql` 与 `docs/数据库设计.md`。
- 外部集成从「模拟」切换为「真实」时，必须同步更新 README「实现状态与边界」、部署文档与验收报告。
- 生产上线前必须更换数据库密码、Redis 密码、`JWT_SECRET`，关闭 `SEED_DEMO`，并完成备份恢复演练。
