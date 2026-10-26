package com.hotel.saas.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class TenantScope {
    private TenantScope() {
    }

    /** 当前操作的租户：平台端从 X-Tenant-Id 头读取（下钻），租户端取登录租户。 */
    public static Long tenantId() {
        if (!TenantContext.isPlatform()) {
            return TenantContext.tenantId();
        }
        HttpServletRequest req = currentRequest();
        String header = req == null ? null : req.getHeader("X-Tenant-Id");
        if (header == null || header.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new BizException("租户标识不合法");
        }
    }

    public static Long requiredTenantId() {
        Long id = tenantId();
        if (id == null || id == 0) {
            throw new BizException(400, "请先在顶部选择要查看的租户");
        }
        return id;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
