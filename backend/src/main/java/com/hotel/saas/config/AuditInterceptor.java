package com.hotel.saas.config;

import com.hotel.saas.common.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {
    private final JdbcTemplate jdbc;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        boolean failed = response.getStatus() >= 400;
        if ("GET".equalsIgnoreCase(method) && !failed) {
            return;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/public")) {
            return;
        }
        try {
            String module = uri.startsWith("/api/") ? uri.substring(5).split("/")[0] : "system";
            String detail = "status=" + response.getStatus() + (ex == null ? "" : ";error=" + ex.getMessage());
            jdbc.update("INSERT INTO sys_audit_log(tenant_id,user_id,username,module,action,target,ip,detail) VALUES(?,?,?,?,?,?,?,?)",
                    TenantContext.tenantId(), TenantContext.userId(), TenantContext.username(),
                    module, method + " " + uri, uri, request.getRemoteAddr(), truncate(detail, 900));
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    private String truncate(String s, int len) {
        return s == null || s.length() <= len ? s : s.substring(0, len);
    }
}
