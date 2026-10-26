package com.hotel.saas.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class ParamConfigController {
    private final ParamMapper paramMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final TenantMapper tenantMapper;

    @GetMapping("/api/admin/params")
    public ApiResult<List<Param>> params() {
        return ApiResult.ok(paramMapper.selectList(new LambdaQueryWrapper<Param>().orderByAsc(Param::getId)));
    }

    @PutMapping("/api/platform/params")
    @Transactional
    public ApiResult<Void> updateParams(@RequestBody List<Param> params) {
        AuthGuard.requirePlatform();
        for (Param p : params) {
            if (p.getId() == null) continue;
            Param db = paramMapper.selectById(p.getId());
            if (db != null) {
                db.setParamValue(p.getParamValue());
                paramMapper.updateById(db);
            }
        }
        return ApiResult.ok();
    }

    @GetMapping("/api/admin/tenant-config")
    public ApiResult<Map<String, String>> tenantConfig() {
        Long tid = TenantScope.requiredTenantId();
        Map<String, String> map = new LinkedHashMap<>();
        tenantConfigMapper.selectList(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tid)).forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return ApiResult.ok(map);
    }

    @PutMapping("/api/admin/tenant-config")
    @Transactional
    public ApiResult<Void> saveTenantConfig(@RequestBody Map<String, String> body) {
        Long tid = TenantScope.requiredTenantId();
        Tenant tenant = tenantMapper.selectById(tid);
        if (tenant == null) throw new BizException(404, "租户不存在");
        body.forEach((k, v) -> {
            TenantConfig c = tenantConfigMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                    .eq(TenantConfig::getTenantId, tid).eq(TenantConfig::getConfigKey, k).last("limit 1"));
            if (c == null) {
                c = new TenantConfig();
                c.setTenantId(tid);
                c.setConfigKey(k);
                c.setConfigValue(v);
                tenantConfigMapper.insert(c);
            } else {
                c.setConfigValue(v);
                tenantConfigMapper.updateById(c);
            }
        });
        return ApiResult.ok();
    }

    @GetMapping("/api/admin/languages")
    public ApiResult<List<Language>> languages() {
        return ApiResult.ok(languageMapper.selectList(new LambdaQueryWrapper<Language>().orderByAsc(Language::getSort)));
    }

    private final LanguageMapper languageMapper;
    private final I18nMapper i18nMapper;

    @GetMapping("/api/admin/i18n")
    public ApiResult<List<I18n>> i18n(@RequestParam(required = false) String lang) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<I18n> w = new LambdaQueryWrapper<I18n>()
                .eq(I18n::getTenantId, tid).orderByAsc(I18n::getMsgKey);
        if (lang != null && !lang.isBlank()) w.eq(I18n::getLang, lang);
        return ApiResult.ok(i18nMapper.selectList(w));
    }

    @PostMapping("/api/admin/i18n")
    public ApiResult<I18n> saveI18n(@RequestBody I18n i18n) {
        Long tid = TenantScope.requiredTenantId();
        I18n db = i18nMapper.selectOne(new LambdaQueryWrapper<I18n>()
                .eq(I18n::getTenantId, tid).eq(I18n::getLang, i18n.getLang())
                .eq(I18n::getMsgKey, i18n.getMsgKey()).last("limit 1"));
        if (db == null) {
            i18n.setId(null);
            i18n.setTenantId(tid);
            i18nMapper.insert(i18n);
            return ApiResult.ok(i18n);
        }
        db.setMsgValue(i18n.getMsgValue());
        i18nMapper.updateById(db);
        return ApiResult.ok(db);
    }

    @DeleteMapping("/api/admin/i18n/{id}")
    public ApiResult<Void> deleteI18n(@PathVariable Long id) {
        i18nMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PutMapping("/api/platform/languages/{id}/enabled")
    public ApiResult<Language> toggleLanguage(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        AuthGuard.requirePlatform();
        Language l = languageMapper.selectById(id);
        if (l == null) throw new BizException(404, "语言不存在");
        l.setEnabled(body.getOrDefault("enabled", 1));
        languageMapper.updateById(l);
        return ApiResult.ok(l);
    }
}
