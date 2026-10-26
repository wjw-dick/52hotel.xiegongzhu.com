package com.hotel.saas.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.NotifyTemplate;
import com.hotel.saas.domain.Notification;
import com.hotel.saas.domain.WechatConfig;
import com.hotel.saas.mapper.NotifyTemplateMapper;
import com.hotel.saas.mapper.NotificationMapper;
import com.hotel.saas.mapper.WechatConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class WechatController {
    private final WechatConfigMapper wechatMapper;
    private final NotifyTemplateMapper templateMapper;
    private final NotificationMapper notificationMapper;
    private final CryptoService crypto;

    @GetMapping("/api/admin/wechat")
    public ApiResult<List<Map<String, Object>>> configs() {
        Long tid = TenantScope.requiredTenantId();
        List<WechatConfig> list = wechatMapper.selectList(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, tid).orderByAsc(WechatConfig::getId));
        return ApiResult.ok(list.stream().map(this::safe).toList());
    }

    @Data
    public static class WechatReq {
        private String appType;
        private String appId;
        private String appSecret;
        private String token;
        private String aesKey;
        private String domain;
        private Integer enabled = 1;
    }

    @PostMapping("/api/admin/wechat")
    public ApiResult<WechatConfig> save(@RequestBody WechatReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getAppType() == null) throw new BizException("请选择应用类型");
        WechatConfig c = wechatMapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, tid).eq(WechatConfig::getAppType, req.getAppType()).last("limit 1"));
        if (c == null) {
            c = new WechatConfig();
            c.setTenantId(tid);
            c.setAppType(req.getAppType());
        }
        c.setAppId(req.getAppId());
        if (req.getAppSecret() != null && !req.getAppSecret().contains("******")) {
            c.setAppSecret(crypto.encrypt(req.getAppSecret()));
        }
        if (req.getAesKey() != null && !req.getAesKey().contains("******")) {
            c.setAesKey(crypto.encrypt(req.getAesKey()));
        }
        c.setToken(req.getToken());
        c.setDomain(req.getDomain());
        c.setEnabled(req.getEnabled());
        if (c.getId() == null) wechatMapper.insert(c); else wechatMapper.updateById(c);
        return ApiResult.ok(c);
    }

    private Map<String, Object> safe(WechatConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("appType", c.getAppType());
        m.put("appId", c.getAppId());
        m.put("appSecretMask", maskSecret(c.getAppSecret()));
        m.put("token", c.getToken());
        m.put("aesKeyMask", maskSecret(c.getAesKey()));
        m.put("domain", c.getDomain());
        m.put("enabled", c.getEnabled());
        m.put("updateTime", c.getUpdateTime());
        return m;
    }

    private String maskSecret(String cipher) {
        String plain = crypto.decrypt(cipher);
        if (plain == null || plain.isBlank()) return "";
        if (plain.length() <= 6) return "******";
        return plain.substring(0, 3) + "******" + plain.substring(plain.length() - 3);
    }

    @GetMapping("/api/admin/notify-templates")
    public ApiResult<List<NotifyTemplate>> templates() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(templateMapper.selectList(new LambdaQueryWrapper<NotifyTemplate>()
                .eq(NotifyTemplate::getTenantId, tid).orderByAsc(NotifyTemplate::getId)));
    }

    @PostMapping("/api/admin/notify-templates")
    public ApiResult<NotifyTemplate> saveTemplate(@RequestBody NotifyTemplate t) {
        Long tid = TenantScope.requiredTenantId();
        t.setTenantId(tid);
        if (t.getId() == null) {
            templateMapper.insert(t);
        } else {
            NotifyTemplate db = templateMapper.selectById(t.getId());
            if (db == null || !db.getTenantId().equals(tid)) throw new BizException(404, "模板不存在");
            templateMapper.updateById(t);
        }
        return ApiResult.ok(t);
    }

    @GetMapping("/api/admin/notifications")
    public ApiResult<List<Notification>> notifications() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTenantId, tid).orderByDesc(Notification::getId).last("limit 100")));
    }

    @PostMapping("/api/admin/notifications/{id}/read")
    public ApiResult<Void> read(@PathVariable Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) throw new BizException(404, "消息不存在");
        n.setStatus("READ");
        notificationMapper.updateById(n);
        return ApiResult.ok();
    }
}
