# API 接口文档 · 云宿 SaaS

> 对应任务：T73；Base URL：`http(s)://<host>/api`。
> 本文按当前代码实际路由整理。请求/响应均为 `application/json`，文件上传为 `multipart/form-data`，CSV 导出为 `text/csv`。

---

## 1. 统一约定

### 1.1 响应结构

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | `0` 成功；非 0 失败（业务码见下表） |
| `msg` | string | 提示信息 |
| `data` | any | 业务数据，可能为对象、数组、分页对象或 `null` |

分页结构：

```json
{
  "records": [],
  "total": 100,
  "current": 1,
  "size": 10
}
```

> 鉴权失败与权限拦截由 Servlet 拦截器直接写 HTTP `401/403`，响应体同样为上述结构；业务异常通常返回 HTTP 200 + 非 0 `code`。前端应同时判断 HTTP 状态与 `code`。

### 1.2 鉴权

| 主体 | 登录方式 | Token 载荷 |
|---|---|---|
| 平台超管 | `POST /auth/login`，**不传** `tenantCode` | `platform=true`、`tenantId=0`、角色 `PLATFORM_ADMIN` |
| 租户用户 | `POST /auth/login` + `tenantCode` | `tenantId`、`tenantCode`、`role`、可选 `storeId` |
| 会员 | `POST /auth/wechat-login` | `role=MEMBER`、`tenantId`、`memberId` |

后续请求头：

```http
Authorization: Bearer <JWT>
Content-Type: application/json
```

### 1.3 特殊请求头

| 请求头 | 使用场景 | 说明 |
|---|---|---|
| `X-Tenant-Id` | 平台超管下钻租户 | 临时切换租户上下文，仅 `/api/admin/**` GET 生效 |
| `X-Request-Id` | 公开下单防重 | 相同值在短时间内重复提交返回 `429` |
| `X-Forwarded-For` | 审计/限流 | 由 Nginx 注入，后端记录真实 IP |

### 1.4 错误码

| code | HTTP | 含义 | 处理建议 |
|---|---|---|---|
| `0` | 200 | 成功 | - |
| `400` | 200/400 | 参数错误/业务校验失败 | 展示 `msg`，修正入参 |
| `401` | 401 | 未登录或 Token 失效 | 跳转登录，清理本地 Token |
| `403` | 200/403 | 无权限/跨租户/租户停用/到期 | 禁止操作；联系管理员 |
| `404` | 200 | 资源不存在 | 刷新列表 |
| `429` | 200/429 | 触发限流/重复提交 | 稍后重试，勿重复点击 |
| `500` | 200/500 | 未处理异常 | 记录请求时间，查后端日志 |

### 1.5 租户编码

公开接口路径中 `{tenantCode}` 为租户编码，例如演示租户 `YQ-HZ`、`XH-SH`。

---

## 2. 认证与账号

| 方法 | 路径 | 鉴权 | 入参 | 出参 | 说明 |
|---|---|---|---|---|---|
| POST | `/auth/login` | 公开 | `username`、`password`、`tenantCode?` | `token`、`userId`、`username`、`role`、`tenantId`、`tenantCode`、`platform` | 平台/租户登录 |
| POST | `/auth/wechat-login` | 公开 | `tenantCode`、`openId?`、`unionId?`、`nickname?`、`phone?` | `token`、`member`、`tenantId`、`tenantCode`、`tenantName` | 演示：直接传 openId；真实需换 code2Session |
| GET | `/auth/me` | 登录 | - | 当前用户资料 | 平台/租户/会员通用 |
| POST | `/auth/password` | 登录 | `oldPassword`、`newPassword` | `null` | 平台/租户用户改密；会员请用 `/member/password` |
| POST | `/member/password` | 会员 | `code`（手机后 6 位）、`newPassword` | `null` | 演示验证码；真实接短信 |

登录示例：

```bash
curl -X POST http://127.0.0.1:19090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","tenantCode":"YQ-HZ"}'
```

---

## 3. 公开门户（H5 / 小程序 / App）

> 无需 Token；后端自动按 `tenantCode` 设置租户上下文。

| 方法 | 路径 | 入参 | 出参 | 说明 |
|---|---|---|---|---|
| GET | `/public/{tenantCode}/home` | - | `tenant`、`stores`、`roomTypes`、`menus`、`config`、`notices` | 首页聚合 |
| GET | `/public/{tenantCode}/stores/{storeId}` | - | `store`、`roomTypes`、`menus` | 门店门户 |
| GET | `/public/{tenantCode}/room-types/{id}` | - | `roomType`、`store`、`rates`、`facilities` | 房型详情 |
| GET | `/public/{tenantCode}/availability` | `roomTypeId`、`checkIn`、`checkOut` | `available`、`roomCount`、`price`、`nights` | 可用房与报价 |
| POST | `/public/{tenantCode}/orders` | 见下方 | `Order` | 公开下单 |
| GET | `/public/{tenantCode}/orders/query` | `orderNo`、`phone` | 订单 + 住客 + 门店 + 房型 | 订单查询，手机号必须匹配 |
| GET | `/public/{tenantCode}/menus` | `platform=H5` | 菜单数组 | H5 底部导航 |
| GET | `/public/{tenantCode}/i18n` | `lang=zh-CN` | `{key: value}` | 门户多语言词条 |

公开下单请求：

```json
{
  "storeId": 1,
  "roomTypeId": 4,
  "checkIn": "2026-11-10",
  "checkOut": "2026-11-12",
  "roomCount": 1,
  "guestName": "张三",
  "guestPhone": "13800009999",
  "idNo": "330106199001011234",
  "source": "ONLINE",
  "channel": "H5",
  "payType": "ARRIVAL",
  "remark": "需要安静房间"
}
```

---

## 4. 平台端（`/platform/**`，仅平台超管）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/platform/dashboard` | - | 全平台看板：租户数/房间/入住率/订单/营收/会员/租户对比/趋势/到期预警 |
| GET | `/platform/tenants` | `page`、`size`、`keyword`、`status` | 租户分页（含房间/用户/订单/会员数、到期标记） |
| POST | `/platform/tenants` | `code`、`name`、`type`、`status`、`contact`、`phone`、`openTime`、`expireTime`、`maxUsers`、`maxRooms`、`adminUsername`、`adminPassword` | 开通租户并创建租户管理员 |
| PUT | `/platform/tenants/{id}` | 同上（编码不可改） | 编辑租户/配额/到期 |
| POST | `/platform/tenants/{id}/status` | `{"status":"NORMAL|DISABLED|TRIAL"}` | 停用/启用 |
| POST | `/platform/tenants/{id}/renew` | `{"months":12}` | 续期 |
| GET | `/platform/tenants/options` | - | 租户下拉选项 |
| GET | `/platform/dicts` | `type?` | 全局字典列表 |
| POST | `/platform/dicts` | `type`、`code`、`name`、`sort` | 新增全局字典 |
| PUT | `/platform/dicts/{id}` | 同上 | 修改 |
| DELETE | `/platform/dicts/{id}` | - | 删除 |
| PUT | `/platform/params` | `Param[]` | 批量保存平台参数 |
| GET | `/platform/audits` | `page`、`size`、`tenantId?`、`keyword?` | 跨租户审计 |
| PUT | `/platform/languages/{id}/enabled` | `{"enabled":1}` | 启用/停用语言 |

平台下钻：

```bash
curl 'http://127.0.0.1:19090/api/admin/orders?page=1&size=20' \
  -H 'Authorization: Bearer <平台Token>' \
  -H 'X-Tenant-Id: 2'
```

> 平台 Token 对 `/api/admin/**` 仅允许 GET；POST/PUT/DELETE 返回 403。

---

## 5. 租户后台（`/admin/**`）

### 5.1 经营看板与报表

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/dashboard` | - | 今日到店/离店/在住/空房/脏房/待办/今日/本月营收/7 日趋势/会员数/租户到期日 |
| GET | `/admin/reports/summary` | `startDate?`、`endDate?` | 营收/订单/间夜/入住率/ADR/RevPAR/来源/房型/门店/趋势 |
| GET | `/admin/reports/export` | `startDate?`、`endDate?` | CSV 导出（UTF-8 BOM） |

### 5.2 门店 / 房型 / 房间 / 房价 / 房态

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/stores` | - | 门店列表 |
| POST | `/admin/stores` | `Store` | 新增门店 |
| PUT | `/admin/stores/{id}` | `Store` | 修改 |
| DELETE | `/admin/stores/{id}` | - | 逻辑删除（有房间/订单时服务层校验） |
| GET | `/admin/stores/{id}/summary` | - | 门店房量/房态/订单/营收摘要 |
| GET | `/admin/room-types` | `storeId?` | 房型列表 |
| POST | `/admin/room-types` | `RoomType` | 新增 |
| PUT | `/admin/room-types/{id}` | `RoomType` | 修改 |
| DELETE | `/admin/room-types/{id}` | - | 删除 |
| GET | `/admin/room-types/{id}/detail` | - | 房型 + 门店 + 房价 + 房间 |
| GET | `/admin/rooms` | `storeId?`、`roomTypeId?`、`status?`、`keyword?` | 房间列表 |
| POST | `/admin/rooms` | `Room` | 新增（校验租户房间配额） |
| PUT | `/admin/rooms/{id}` | `Room` | 修改 |
| DELETE | `/admin/rooms/{id}` | - | 删除 |
| POST | `/admin/rooms/{id}/status` | `status`、`remark?` | 改房态并写日志 |
| GET | `/admin/rooms/{id}/status-logs` | - | 房态变更记录 |
| GET | `/admin/rooms/export` | - | 房间 CSV 导出 |
| POST | `/admin/rooms/import` | `file`（CSV） | 房间批量导入，返回成功/失败行 |
| GET | `/admin/rates` | `roomTypeId`、`month=YYYY-MM` | 房价日历 |
| POST | `/admin/rates/batch` | `roomTypeId`、`startDate`、`endDate`、`priceType`、`price` | 批量改价（最长 6 个月），逐条写改价日志 |
| GET | `/admin/rates/logs` | `roomTypeId?` | 改价记录（最近 200 条） |

房态枚举：`VACANT`（空房）、`RESERVED`（预订）、`OCCUPIED`（在住）、`DIRTY`（脏房）、`REPAIR`（维修）、`CLOSED`（关闭）。
房价类型：`BAR`（门市）、`CHANNEL`（渠道）、`AGREEMENT`（协议）。

### 5.3 预订 / 订单

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/orders` | `page`、`size`、`keyword?`、`status?`、`source?`、`storeId?`、`startDate?`、`endDate?` | 订单分页 |
| GET | `/admin/orders/{id}` | - | 订单详情（住客/入住/结算/日志） |
| POST | `/admin/orders` | `OrderDtos.CreateReq` | 后台下单 |
| POST | `/admin/orders/{id}/cancel` | `reason` | 取消并释放房间 |
| POST | `/admin/orders/{id}/checkin` | `roomId?`、`guestName`、`idNo`、`deposit`、`depositPayType`、`cardNo?`、`minorInfo?` | 入住登记（实名必填） |
| POST | `/admin/orders/{id}/checkout` | `discount?`、`otherFee?`、`depositDeduct?`、`payType`、`remark?` | 退房结算 |
| POST | `/admin/orders/{id}/change-room` | `roomId`、`remark?` | 换房 |
| POST | `/admin/orders/{id}/extend` | `newCheckOut`、`remark?` | 续住 |
| POST | `/admin/orders/{id}/deposit` | `amount`、`type`（COLLECT/REFUND）、`payType` | 补收/退押金 |
| GET | `/admin/orders/availability` | `roomTypeId`、`checkIn`、`checkOut` | 可用房校验 |
| GET | `/admin/orders/today` | - | 今日到店/离店/在住 |

下单请求（后台）：

```json
{
  "storeId": 2,
  "roomTypeId": 4,
  "checkIn": "2026-11-20",
  "checkOut": "2026-11-22",
  "guestName": "李四",
  "guestPhone": "13800008888",
  "idNo": "330106199001011234",
  "source": "WALK_IN",
  "payType": "ARRIVAL",
  "remark": "自动化流程"
}
```

### 5.4 前台接待

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/checkins` | `status?` | 入住登记列表 |
| GET | `/admin/checkins/{id}/changes` | - | 续住/换房/提前退/延时退记录 |
| GET | `/admin/police-reports` | `status?` | 公安报送记录 |
| POST | `/admin/police-reports/{id}/retry` | - | 重新报送（当前为模拟成功） |

### 5.5 房务保洁

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/hk/overview` | - | 待清扫/处理中/已完成/脏房/报修待处理统计 |
| GET | `/admin/hk/tasks` | `status?`、`type?` | 清扫任务（含房间/门店） |
| POST | `/admin/hk/tasks` | `roomId`、`type`、`assignee?`、`note?` | 创建任务（房间置 DIRTY） |
| POST | `/admin/hk/tasks/{id}/status` | `status`、`markClean?` | 更新任务；完成并置净时房间转 VACANT |
| POST | `/admin/hk/rooms/{roomId}/clean` | `remark?` | 脏房直接置净 |
| GET | `/admin/hk/repairs` | `status?` | 报修工单 |
| POST | `/admin/hk/repairs` | `roomId?`、`title`、`images?`、`storeId?` | 报修 |
| POST | `/admin/hk/repairs/{id}/status` | `status`、`assignee?` | 流转 |
| GET | `/admin/hk/linen` | - | 布草记录 |
| POST | `/admin/hk/linen` | `storeId`、`type`、`item`、`quantity` | 领用/送洗/归还 |
| GET | `/admin/hk/complaints` | `status?` | 客诉列表 |
| POST | `/admin/hk/complaints` | `storeId`、`guest`、`content`、`roomNo?` | 登记客诉 |
| POST | `/admin/hk/complaints/{id}/status` | `status`、`handler?` | 处理客诉 |

任务状态：`PENDING` → `PROCESSING` → `DONE`。
报修状态：`REPORTED` → `ASSIGNED` → `REPAIRING` → `ACCEPTING` → `DONE`。

### 5.6 财务

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/finance/overview` | - | 今日/本月营收、结算数、押金、挂账余额 |
| GET | `/admin/finance/settlements` | `startDate?`、`endDate?` | 结算单列表 |
| GET | `/admin/finance/cashier` | - | 收银流水 |
| GET | `/admin/finance/deposits` | - | 押金记录 |
| GET | `/admin/finance/ar` | - | 挂账客户 |
| POST | `/admin/finance/ar` | `company`、`contact`、`phone`、`creditLimit?` | 新增挂账客户 |
| GET | `/admin/finance/ar/{id}/ledger` | - | 挂账流水 |
| POST | `/admin/finance/ar/{id}/trade` | `type`（CHARGE/VERIFY）、`amount`、`remark?` | 挂账/核销 |
| GET | `/admin/finance/shifts` | - | 交接班列表 |
| POST | `/admin/finance/shifts/open` | `storeId`、`cashAmount`、`shiftName?` | 开班（已有 OPEN 班次返回 400） |
| POST | `/admin/finance/shifts/{id}/close` | `cashAmount?` | 交班 |
| GET | `/admin/finance/stores` | - | 门店选项 |

### 5.7 会员

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/members` | `keyword?`、`levelId?` | 会员列表 |
| GET | `/admin/members/levels` | - | 会员等级 |
| POST | `/admin/members` | `name`、`phone`、`gender?`、`tags?` | 新增会员 |
| PUT | `/admin/members/{id}` | 同上 | 修改 |
| DELETE | `/admin/members/{id}` | - | 删除 |
| GET | `/admin/members/{id}/detail` | - | 会员 + 订单 + 积分 + 优惠券 |
| POST | `/admin/members/{id}/points` | `changeValue`、`reason` | 调整积分（写流水） |
| POST | `/admin/members/{id}/coupons` | `name`、`amount`、`expireDate` | 发券 |
| POST | `/admin/members/levels/refresh` | - | 按累计消费重算等级 |

### 5.8 系统配置 / 用户 / 审计

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/params` | - | 平台参数（租户可读） |
| GET | `/admin/tenant-config` | - | 本租户配置 `{key:value}` |
| PUT | `/admin/tenant-config` | `{key:value}` | 保存租户配置（租户管理员） |
| GET | `/admin/languages` | - | 语言列表 |
| GET | `/admin/i18n` | `lang?` | 词条列表 |
| POST | `/admin/i18n` | `lang`、`msgKey`、`msgValue` | 新增/更新词条 |
| DELETE | `/admin/i18n/{id}` | - | 删除词条 |
| GET | `/admin/menus` | `platform?` | 菜单配置（ADMIN/MINI/H5/APP） |
| POST | `/admin/menus` | `MenuConfig` | 新增菜单 |
| PUT | `/admin/menus/{id}` | `MenuConfig` | 修改 |
| DELETE | `/admin/menus/{id}` | - | 删除 |
| PUT | `/admin/menus/sort` | `[{id,sort}]` | 排序 |
| GET | `/admin/users` | - | 租户用户列表 |
| GET | `/admin/roles` | - | 角色列表 |
| POST | `/admin/users` | `username`、`password`、`realName`、`phone`、`roleCode`、`storeId?` | 新增用户（用户配额校验） |
| PUT | `/admin/users/{id}` | 同上（密码可空） | 修改 |
| POST | `/admin/users/{id}/reset-password` | `password` | 重置密码 |
| DELETE | `/admin/users/{id}` | - | 删除（不可删自己） |
| GET | `/admin/audits` | `page`、`size`、`module?`、`keyword?` | 本租户审计 |
| GET | `/admin/storage` | - | 存储配置（密钥掩码） |
| POST | `/admin/storage` | `driver`、`config{}` | 保存（AK/SK 加密） |
| POST | `/admin/storage/test` | `driver`、`config{}` | 连接测试 |
| POST | `/admin/files/upload` | `file`、`category?` | 上传文件 |
| DELETE | `/admin/files` | `url` | 删除本地文件 |
| GET | `/admin/wechat` | - | 微信配置（密钥掩码） |
| POST | `/admin/wechat` | `appType`、`appId`、`appSecret?`、`token?`、`aesKey?`、`domain?`、`enabled?` | 保存微信配置 |
| GET | `/admin/notify-templates` | - | 消息模板 |
| POST | `/admin/notify-templates` | `bizType`、`channel`、`templateId`、`title`、`content`、`enabled` | 保存模板 |
| GET | `/admin/notifications` | - | 站内消息（最近 100 条） |
| POST | `/admin/notifications/{id}/read` | - | 标记已读 |
| GET | `/admin/dicts` | `type?` | 全局字典 + 租户扩展 |
| POST | `/admin/dicts` | `type`、`code`、`name` | 新增租户扩展项 |
| DELETE | `/admin/dicts/{id}` | - | 删除租户扩展项 |

### 5.9 增强模块（演示）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/admin/enhance/channels` | - | OTA 渠道列表 |
| POST | `/admin/enhance/channels` | `name`、`code`、`syncStock?` | 新增渠道 |
| POST | `/admin/enhance/channels/{id}/sync` | - | 手动同步（写 `last_sync`，模拟） |
| GET | `/admin/enhance/dining` | - | 餐饮订单 |
| POST | `/admin/enhance/dining` | `storeId`、`roomNo`、`items`、`amount` | 新建餐饮订单 |
| POST | `/admin/enhance/dining/{id}/charge-room` | - | 挂账到房（更新结算方式） |
| POST | `/admin/enhance/rooms/{id}/unlock` | - | 远程开门（模拟） |

---

## 6. 会员中心（`/member/**`，仅 MEMBER）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/member/profile` | - | 会员 + 等级 + 可用券数；手机号掩码 |
| PUT | `/member/profile` | `name`、`phone`、`gender` | 修改资料 |
| GET | `/member/orders` | - | 本人订单 |
| POST | `/member/orders/{id}/cancel` | `reason?` | 取消本人可取消订单 |
| GET | `/member/points` | - | 积分与流水 |
| GET | `/member/coupons` | - | 优惠券 |
| GET | `/member/notifications` | - | 站内消息 |
| POST | `/member/password` | `code`、`newPassword` | 改密（演示验证码=手机后 6 位） |

---

## 7. 健康检查与文件

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/health` | 公开 | `{service,time,status}`，用于探活 |
| GET | `/uploads/**` | 公开 | 本地存储文件访问（生产建议 Nginx 直出） |

---

## 8. 典型调用顺序

### 8.1 租户管理员

```text
POST /auth/login (tenantCode=YQ-HZ)
→ GET /auth/me
→ GET /admin/dashboard
→ GET /admin/stores /room-types /rooms
→ GET /admin/orders
→ GET /admin/reports/summary
```

### 8.2 前台

```text
GET /admin/orders?status=CONFIRMED
→ POST /admin/orders/{id}/checkin
→ GET /admin/checkins?status=IN_HOUSE
→ POST /admin/orders/{id}/deposit
→ POST /admin/orders/{id}/checkout
```

### 8.3 保洁

```text
GET /admin/hk/overview
→ GET /admin/hk/tasks?status=PENDING
→ POST /admin/hk/tasks/{id}/status {"status":"DONE","markClean":true}
→ GET /admin/rooms?status=VACANT
```

### 8.4 C 端

```text
GET /public/YQ-HZ/home
→ GET /public/YQ-HZ/availability
→ POST /public/YQ-HZ/orders
→ GET /public/YQ-HZ/orders/query
→ POST /auth/wechat-login
→ GET /member/profile
```

---

## 9. 接口与权限对照

| 路径前缀 | 平台超管 | 租户管理员 | 前台 | 保洁 | 财务 | 会员 |
|---|---|---|---|---|---|---|
| `/api/platform/**` | ✅ | ❌ 403 | ❌ | ❌ | ❌ | ❌ |
| `/api/admin/**` GET | ✅ 只读下钻 | ✅ | 按模块 | 按模块 | 按模块 | ❌ |
| `/api/admin/**` 写 | ❌ 403 | ✅ | 按模块 | 按模块 | 按模块 | ❌ |
| `/api/member/**` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ 本人 |
| `/api/public/**` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 具体模块角色矩阵见《SaaS隔离与安全.md》与 `AccessInterceptor`。
