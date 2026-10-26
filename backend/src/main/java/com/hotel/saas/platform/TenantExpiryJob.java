package com.hotel.saas.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.domain.Notification;
import com.hotel.saas.domain.Tenant;
import com.hotel.saas.mapper.NotificationMapper;
import com.hotel.saas.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 租户到期治理：每日检查到期租户并自动停用；对 30 天内到期租户写入站内预警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpiryJob {
    private final TenantMapper tenantMapper;
    private final NotificationMapper notificationMapper;

    @Scheduled(cron = "0 10 1 * * ?")
    public void checkTenants() {
        LocalDate today = LocalDate.now();
        List<Tenant> expiring = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .in(Tenant::getStatus, "TRIAL", "NORMAL")
                .le(Tenant::getExpireTime, today.plusDays(30)));
        for (Tenant t : expiring) {
            if (t.getExpireTime() != null && t.getExpireTime().isBefore(today)) {
                t.setStatus("DISABLED");
                tenantMapper.updateById(t);
                log.warn("租户已到期自动停用: {} {}", t.getCode(), t.getName());
            } else {
                Notification n = new Notification();
                n.setTenantId(t.getId());
                n.setTitle("租户即将到期");
                n.setContent("租户 " + t.getName() + " 将于 " + t.getExpireTime() + " 到期，请及时续期。");
                n.setChannel("SITE");
                n.setStatus("SENT");
                notificationMapper.insert(n);
            }
        }
    }
}
