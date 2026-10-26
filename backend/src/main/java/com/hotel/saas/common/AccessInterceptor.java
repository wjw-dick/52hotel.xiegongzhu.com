package com.hotel.saas.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 两级权限拦截：平台端下钻只读、租户角色按模块授权。
 * JWT 解析由 AuthInterceptor 先行完成，本拦截器只做角色/模块校验。
 */
@Component
public class AccessInterceptor implements HandlerInterceptor {
    private static final Set<String> ADMIN_ONLY = Set.of("TENANT_ADMIN");
    private static final Set<String> ALL_TENANT_ROLES = Set.of(
            "TENANT_ADMIN", "STORE_MANAGER", "FRONT_DESK", "HOUSEKEEPING", "FINANCE", "OTA_OPERATOR");
    private static final Set<String> FRONT = Set.of("TENANT_ADMIN", "STORE_MANAGER", "FRONT_DESK");
    private static final Set<String> HOUSE = Set.of("TENANT_ADMIN", "STORE_MANAGER", "FRONT_DESK", "HOUSEKEEPING");
    private static final Set<String> FINANCE = Set.of("TENANT_ADMIN", "STORE_MANAGER", "FINANCE");
    private static final Set<String> MEMBER_OPS = Set.of("TENANT_ADMIN", "STORE_MANAGER", "FRONT_DESK", "OTA_OPERATOR");
    private static final Set<String> ENHANCE = Set.of("TENANT_ADMIN", "STORE_MANAGER", "OTA_OPERATOR");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) return true;
        TenantContext.User user = TenantContext.get();
        if (user == null) return true; // 公共接口或已由 AuthInterceptor 返回 401

        if (path.startsWith("/api/platform/")) {
            if (!user.platform()) return deny(response, "仅平台超级管理员可操作");
            return true;
        }

        if (path.startsWith("/api/admin/") && user.platform()) {
            // 平台端下钻只读，避免误改租户业务数据
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                return deny(response, "平台端下钻仅支持查看，不能修改租户数据");
            }
            return true;
        }

        if (path.startsWith("/api/member/") && !"MEMBER".equals(user.role())) {
            return deny(response, "仅会员账号可访问会员中心");
        }

        if (!path.startsWith("/api/admin/")) return true;
        if ("MEMBER".equals(user.role())) return deny(response, "会员账号无权访问管理后台");

        String module = path.substring("/api/admin/".length());
        int slash = module.indexOf('/');
        if (slash >= 0) module = module.substring(0, slash);

        // 个人消息中心、文件上传为公共能力
        if ("notifications".equals(module) || "files".equals(module)) return true;
        // 渠道/餐饮只读信息在门店页展示，写操作仍限制 OTA/店长/管理员
        if ("enhance".equals(module) && "GET".equalsIgnoreCase(request.getMethod())) return true;

        Set<String> allowed = switch (module) {
            case "users", "audit", "audits", "params", "tenant-config", "wechat", "notify-templates",
                    "languages", "i18n", "menus", "storage" -> ADMIN_ONLY;
            case "finance" -> FINANCE;
            case "hk" -> HOUSE;
            case "orders", "checkins", "police-reports" -> FRONT;
            case "members" -> MEMBER_OPS;
            case "enhance" -> ENHANCE;
            case "stores", "room-types", "rooms", "rates", "dashboard", "reports", "dicts" -> ALL_TENANT_ROLES;
            default -> ALL_TENANT_ROLES;
        };
        if (!allowed.contains(user.role())) return deny(response, "当前角色无权访问该模块");
        return true;
    }

    private boolean deny(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
