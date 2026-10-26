package com.hotel.saas.common;

import java.util.function.Supplier;

public final class TenantContext {
    public record User(Long userId, String username, Long tenantId, String tenantCode,
                       String role, boolean platform, Long storeId) {
    }

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static Long tenantId() {
        User u = HOLDER.get();
        return u == null ? 0L : u.tenantId() == null ? 0L : u.tenantId();
    }

    public static Long userId() {
        User u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String username() {
        User u = HOLDER.get();
        return u == null ? "anonymous" : u.username();
    }

    public static boolean isPlatform() {
        User u = HOLDER.get();
        return u != null && u.platform();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static <T> T runAsPlatform(Supplier<T> supplier) {
        User old = HOLDER.get();
        try {
            HOLDER.set(new User(0L, "system", 0L, "PLATFORM", "PLATFORM_ADMIN", true, null));
            return supplier.get();
        } finally {
            if (old == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(old);
            }
        }
    }

    public static <T> T runAsTenant(Long tenantId, String tenantCode, String role, Supplier<T> supplier) {
        User old = HOLDER.get();
        try {
            HOLDER.set(new User(null, "guest", tenantId, tenantCode, role, false, null));
            return supplier.get();
        } finally {
            if (old == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(old);
            }
        }
    }
}
