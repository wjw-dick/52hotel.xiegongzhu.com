# SaaS 隔离与安全说明 · 云宿 SaaS

> 对应交付物：SaaS 多租户隔离说明文档；对应任务：T03、T23、T54、T55。
> 本文以当前代码为准，说明**隔离机制、权限模型、审计、安全基线、已验证证据与剩余风险**。

---

## 1. 隔离模型总览

```text
请求
 │
 ├─ AuthInterceptor：校验 Bearer JWT → TenantContext.set(user)
 │
 ├─ AccessInterceptor：平台/租户/会员边界 + 租户角色模块授权
 │
 ├─ Service：显式租户校验（如订单 mustGet、房间配额、会员本人）
 │
 ├─ TenantLineInnerInterceptor：SQL 自动注入 tenant_id
 │
 └─ AuditInterceptor：写操作/失败请求/403 落审计
```

四层防护：**身份 → 角色 → 服务校验 → SQL 注入**。任何一层失效，其余层仍可阻断常见越权。

---

## 2. 租户上下文

### 2.1 数据结构

`TenantContext.User`：

```java
record User(Long userId, String username, Long tenantId, String tenantCode,
            String role, boolean platform, Long storeId)
```

- 存储于 `ThreadLocal`，请求结束由 `AuthInterceptor.afterCompletion` 清理。
- 平台主体：`platform=true`、`tenantId=0`、`role=PLATFORM_ADMIN`。
- 租户主体：`platform=false`、`tenantId=<租户ID>`、`role=<角色>`。
- 会员主体：`role=MEMBER`、`tenantId=<租户ID>`、`userId=<会员ID>`。

### 2.2 临时切换

| 场景 | 方法 | 说明 |
|---|---|---|
| 公开门户 | `TenantContext.runAsTenant(tenantId, code, "GUEST", supplier)` | 无登录，按路径 `tenantCode` 设置租户上下文 |
| 平台下钻 | `TenantScope.tenantId()` 读取 `X-Tenant-Id` | 平台 Token + 请求头，仅 GET 放行 |
| 系统任务 | `TenantContext.runAsPlatform(...)` | 定时任务/初始化跨租户操作 |

> `runAsTenant`/`runAsPlatform` 使用 `try/finally` 恢复原上下文，避免线程池串租户。

---

## 3. MyBatis-Plus 租户插件

### 3.1 配置

```java
new TenantLineInnerInterceptor(new TenantLineHandler() {
    public Expression getTenantId() {
        return new LongValue(TenantContext.tenantId() == 0 ? 0L : TenantContext.tenantId());
    }
    public String getTenantIdColumn() { return "tenant_id"; }
    public boolean ignoreTable(String tableName) {
        return PLATFORM_TABLES.contains(tableName.toLowerCase()) || TenantContext.isPlatform();
    }
});
```

### 3.2 平台表白名单

```text
sys_tenant, sys_platform_user, sys_dict, sys_param, sys_language, sys_menu_config
```

| 表 | 不隔离原因 | 额外保护 |
|---|---|---|
| `sys_tenant` | 平台管理全部租户 | 仅 `/api/platform/**` 访问 |
| `sys_platform_user` | 平台账号 | 登录校验 + 平台接口 |
| `sys_dict` | 全局标准字典 | 租户只读，平台维护 |
| `sys_param` | 平台参数 | 平台写、租户读 |
| `sys_language` | 全局语言 | 平台启停、各端读取 |
| `sys_menu_config` | 按租户/端配置，需按编码查询 | `MenuController` 显式 `tenant_id` 过滤（列表/新增/更新/删除/排序/公开查询） |

### 3.3 新增表规范

- 业务表必须含 `tenant_id BIGINT NOT NULL`，否则插件注入会报未知列（显性失败）。
- 平台表加入白名单必须评审，并在 Controller/Service 显式做权限校验。
- 禁止在业务代码中拼接 SQL 绕过插件；自定义 SQL 必须手写 `tenant_id` 条件。

---

## 4. 两级权限

### 4.1 主体边界

| 路径 | 平台超管 | 租户用户 | 会员 | 公开 |
|---|---|---|---|---|
| `/api/platform/**` | ✅ | ❌ | ❌ | ❌ |
| `/api/admin/**` GET | ✅ 下钻只读 | ✅ 按角色 | ❌ | ❌ |
| `/api/admin/**` 写 | ❌ 403 | ✅ 按角色 | ❌ | ❌ |
| `/api/member/**` | ❌ | ❌ | ✅ 本人 | ❌ |
| `/api/auth/**` 登录 | ✅ | ✅ | ✅ | ✅ |
| `/api/public/**` | ✅ | ✅ | ✅ | ✅ |

### 4.2 租户角色模块矩阵

| 模块前缀 | 租户管理员 | 店长 | 前台 | 保洁 | 财务 | OTA 运营 |
|---|---|---|---|---|---|---|
| `orders` / `checkins` / `police-reports` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| `hk` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| `finance` | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| `members` | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| `enhance` GET | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `enhance` 写 | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `users` / `audit` / `params` / `tenant-config` / `wechat` / `notify-templates` / `languages` / `i18n` / `menus` / `storage` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `stores` / `room-types` / `rooms` / `rates` / `dashboard` / `reports` / `dicts` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 实现：`AccessInterceptor`。前端 `Layout.vue`/`router/index.ts` 使用同一矩阵做菜单隐藏与路由拦截；后端为最终权威，直接调接口同样会被拒。

### 4.3 会员本人隔离

- 会员 JWT 的 `userId` 即 `mem_member.id`。
- `MemberPortalController.current()` 从上下文取会员，订单/积分/券/消息查询均按 `member_id` 过滤。
- 取消订单前校验订单 `memberId` 与当前会员一致。
- 会员账号访问 `/api/admin/**` 返回 403；租户账号访问 `/api/member/**` 返回 403。

---

## 5. 越权与审计

### 5.1 拦截点

| 场景 | 机制 | 结果 |
|---|---|---|
| 租户 A 访问租户 B 订单 | `OrderService.mustGet` 显式比较 `tenantId` | 403 + 审计 |
| 租户用户访问平台接口 | `AccessInterceptor` | 403 |
| 平台账号写租户业务 | `AccessInterceptor` | 403（下钻只读） |
| 前台访问财务 | `AccessInterceptor` 模块矩阵 | 403 |
| 会员访问后台 | `AccessInterceptor` | 403 |
| 无 Token/过期 | `AuthInterceptor` | 401 |
| 租户停用/到期 | `AuthService.checkTenant` | 403 |

### 5.2 审计内容

`sys_audit_log`：

| 字段 | 说明 |
|---|---|
| `tenant_id` | 操作所属租户（平台为 0） |
| `user_id` / `username` | 操作人 |
| `module` | 路径首段（orders/finance/...） |
| `action` | `METHOD URI` |
| `target` | 请求 URI |
| `ip` | 客户端 IP（经 Nginx 时为真实 IP） |
| `detail` | HTTP 状态、异常信息 |
| `create_time` | 时间 |

写入规则：

- 所有非 GET 请求（除登录/公开接口）；
- 所有失败请求（含 GET 4xx/5xx）；
- 订单跨租户访问额外显式写入。

### 5.3 查询

- 租户：`GET /api/admin/audits?page&size&module&keyword`（仅租户管理员）。
- 平台：`GET /api/platform/audits?page&size&tenantId&keyword`。

---

## 6. 认证与密码安全

| 项 | 实现 |
|---|---|
| 密码存储 | BCrypt（`PasswordEncoder`） |
| Token | JWT HS256，默认 12 小时，`JWT_SECRET` 注入 |
| 登录限流 | `RateLimiter` 按 IP+账号 每分钟限制 |
| 改密 | 平台/租户：旧密码校验；会员：手机后 6 位验证码（演示） |
| 密码强度 | 至少 6 位；生产建议前端加复杂度校验 |
| 会话 | 无状态；改密后旧 Token 仍有效至过期（可后续加 Token 版本号） |

> 生产必须更换 `JWT_SECRET`；多实例必须使用同一密钥，否则 Token 互不识别。

---

## 7. 敏感数据保护

| 数据 | 存储 | 展示 |
|---|---|---|
| 身份证号 | `CryptoService` AES 加密（`ENC:` 前缀） | 列表/报送掩码 `前4**********后4` |
| 微信 AppSecret / AES Key | AES 加密 | 页面掩码 `abc******xyz` |
| 对象存储 AK/SK | AES 加密 | 页面掩码 |
| 数据库/Redis 密码 | `.env`（600），不入库 | 不展示 |
| 日志 | 不打印明文证件/密钥 | - |

- 加密密钥由 `app.jwt.secret` 派生（MD5 → AES key）。**更换 JWT_SECRET 会导致历史密文无法解密**，生产变更需先迁移数据或固定独立加密密钥。
- 建议后续拆分 `APP_CRYPTO_SECRET`，与 JWT 密钥解耦。

---

## 8. 接口安全基线

| 风险 | 措施 | 现状 |
|---|---|---|
| 重复下单 | `X-Request-Id` + Redis/内存防重 | 已验证 429 |
| 订单号并发重复 | Redis 原子自增 + `uk_order_no` | 已验证 |
| 暴力登录 | 每分钟限流 | 已实现 |
| 批量撞库 | 限流 + 审计 | 基础能力 |
| 上传攻击 | 大小限制、按租户目录、随机文件名 | 已实现；未做病毒扫描 |
| SQL 注入 | MyBatis-Plus 参数化 + 禁止拼接 | 已实现 |
| XSS | 前端默认转义；后端未做富文本白名单 | 需按需增强 |
| CSRF | JWT 放 Header，非 Cookie | 风险较低 |
| CORS | 当前允许任意来源（开发便利） | **生产需收紧** |
| 越权 | 四层防护 | 已验证主要场景 |
| 敏感信息泄露 | 掩码 + 异常统一提示 | 已实现 |

### 8.1 生产安全加固建议

1. `WebConfig` 的 CORS `allowedOriginPatterns("*")` 改为生产域名白名单，或由 Nginx 统一控制。
2. Nginx 增加限流：`limit_req_zone` 对 `/api/auth/login`、`/api/public/*/orders` 限速。
3. 为登录失败增加账号锁定/验证码。
4. 引入独立加密密钥与密钥轮换方案。
5. 上传增加 MIME/后缀白名单、图片压缩、病毒扫描。
6. 开启 HTTPS/HSTS，关闭不必要端口。
7. 定期审计 `sys_audit_log` 中的 403/429 异常峰值。

---

## 9. 已验证证据（2026-10-26）

| 验证项 | 方法 | 结果 |
|---|---|---|
| 租户 A 看不到租户 B 订单 | 租户 A Token 请求租户 B 订单 ID | 403 |
| 平台可跨租户统计 | 平台 Token 请求 `/platform/dashboard` | 200，含两租户数据 |
| 平台下钻只读 | 平台 Token + `X-Tenant-Id: 2` GET 订单 | 200，返回租户 2 数据 |
| 平台下钻写操作 | 平台 Token POST `/admin/**` | 403 |
| 前台访问财务 | `front` Token GET `/admin/finance/*` | 403 |
| 保洁访问订单 | `house` Token GET `/admin/orders` | 403 |
| 财务访问房务 | `finance` Token GET `/admin/hk/*` | 403 |
| 会员访问后台 | 会员 Token GET `/admin/**` | 403 |
| 公开订单防重 | 相同 `X-Request-Id` 重复提交 | 429 |
| 订单查询手机号校验 | 错误手机号查询 | 403 |
| 租户插件生效 | 两租户数据列表互不包含 | 通过 |
| 审计落库 | 跨租户访问后查询审计 | 有记录 |

> 完整接口回归：92 项通过 / 0 失败（`/tmp/test_api.sh`）。

---

## 10. 已知限制与待办

1. 平台超管跨租户下钻仅支持按 `X-Tenant-Id` 单租户查看，未实现“全部租户合并视图”。
2. `NoGenerator` 在 Redis 不可用时使用雪碧 ID 取模兜底，极端并发仍有理论重复可能；数据库唯一键最终兜底。
3. 会员改密验证码为演示逻辑（手机后 6 位），真实环境必须接短信。
4. 微信登录、支付、公安、OTA、门锁为模拟；切换真实服务时需补充验签、幂等、重试、对账。
5. CORS、密码复杂度、上传白名单、Token 主动失效需按生产环境加固。
6. 审计为同步写入，极高并发下可能成为瓶颈；可改为异步队列。
