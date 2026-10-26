package com.hotel.saas.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {
    private static final class Bucket {
        private final long windowStart;
        private final AtomicInteger count;

        private Bucket(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int limitPerMinute) {
        long now = System.currentTimeMillis() / 60000;
        Bucket b = buckets.compute(key, (k, old) -> {
            if (old == null || old.windowStart != now) return new Bucket(now);
            old.count.incrementAndGet();
            return old;
        });
        if (b.count.get() > limitPerMinute) {
            throw new BizException(429, "操作过于频繁，请稍后再试");
        }
        if (buckets.size() > 10000) {
            buckets.entrySet().removeIf(e -> e.getValue().windowStart < now - 5);
        }
    }
}
