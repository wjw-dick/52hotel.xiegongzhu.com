package com.hotel.saas.common;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class NoGenerator {
    private final StringRedisTemplate redis;

    /** 生成租户内唯一业务编号：租户编码-年份-4位序号，Redis 原子自增。 */
    public String next(String tenantCode, String biz, String datePrefix) {
        String year = String.valueOf(LocalDate.now().getYear());
        String key = "hotel:seq:" + tenantCode + ":" + biz + ":" + year;
        long seq;
        try {
            Long v = redis.opsForValue().increment(key);
            if (v != null && v == 1L) {
                redis.expire(key, Duration.ofDays(40));
            }
            seq = v == null ? fallback() : v;
        } catch (Exception e) {
            seq = fallback();
        }
        return (datePrefix == null ? tenantCode : datePrefix) + "-" + year + "-" + String.format("%04d", seq);
    }

    public String random(String prefix) {
        return prefix + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                + IdUtil.getSnowflakeNextIdStr().substring(10);
    }

    private long fallback() {
        return Math.abs(IdUtil.getSnowflakeNextId()) % 9000 + 1000;
    }
}
