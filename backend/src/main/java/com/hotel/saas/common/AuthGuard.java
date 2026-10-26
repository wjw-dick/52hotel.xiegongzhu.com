package com.hotel.saas.common;

public final class AuthGuard {
    private AuthGuard() {
    }

    public static void requirePlatform() {
        if (!TenantContext.isPlatform()) {
            throw new BizException(403, "仅平台超级管理员可操作");
        }
    }

    public static void requireTenant() {
        if (TenantContext.isPlatform() || TenantContext.tenantId() == 0) {
            throw new BizException(403, "请使用租户账号操作");
        }
    }

    public static void requireRole(String... roles) {
        TenantContext.User u = TenantContext.get();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        for (String r : roles) {
            if (r.equals(u.role())) {
                return;
            }
        }
        throw new BizException(403, "当前角色无权执行该操作");
    }
}
