package com.hotel.saas.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.Dict;
import com.hotel.saas.domain.TenantDict;
import com.hotel.saas.mapper.DictMapper;
import com.hotel.saas.mapper.TenantDictMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DictionaryController {
    private final DictMapper dictMapper;
    private final TenantDictMapper tenantDictMapper;

    @GetMapping("/api/admin/dicts")
    public ApiResult<Map<String, Object>> tenantDicts(@RequestParam(required = false) String type) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<Dict> gw = new LambdaQueryWrapper<Dict>().eq(Dict::getStatus, 1).orderByAsc(Dict::getSort);
        LambdaQueryWrapper<TenantDict> tw = new LambdaQueryWrapper<TenantDict>()
                .eq(TenantDict::getTenantId, tid).orderByAsc(TenantDict::getSort);
        if (type != null && !type.isBlank()) {
            gw.eq(Dict::getType, type);
            tw.eq(TenantDict::getType, type);
        }
        List<Dict> global = dictMapper.selectList(gw);
        List<TenantDict> custom = tenantDictMapper.selectList(tw);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Dict d : global) {
            grouped.computeIfAbsent(d.getType(), k -> new ArrayList<>()).add(Map.of(
                    "code", d.getCode(), "name", d.getName(), "scope", "GLOBAL"));
        }
        for (TenantDict d : custom) {
            grouped.computeIfAbsent(d.getType(), k -> new ArrayList<>()).add(Map.of(
                    "code", d.getCode(), "name", d.getName(), "scope", "TENANT", "id", d.getId()));
        }
        return ApiResult.ok(Map.of("dicts", grouped, "globalTypes",
                global.stream().map(Dict::getType).distinct().toList()));
    }

    @Data
    public static class TenantDictReq {
        private String type;
        private String code;
        private String name;
        private Integer sort = 0;
    }

    @PostMapping("/api/admin/dicts")
    public ApiResult<TenantDict> addTenantDict(@RequestBody TenantDictReq req) {
        if (req.getType() == null || req.getCode() == null || req.getName() == null) {
            throw new BizException("字典类型、编码、名称不能为空");
        }
        Long tid = TenantScope.requiredTenantId();
        Long dup = tenantDictMapper.selectCount(new LambdaQueryWrapper<TenantDict>()
                .eq(TenantDict::getTenantId, tid).eq(TenantDict::getType, req.getType())
                .eq(TenantDict::getCode, req.getCode()));
        if (dup != null && dup > 0) throw new BizException("扩展字典编码已存在");
        TenantDict d = new TenantDict();
        d.setTenantId(tid);
        d.setType(req.getType());
        d.setCode(req.getCode());
        d.setName(req.getName());
        d.setSort(req.getSort());
        tenantDictMapper.insert(d);
        return ApiResult.ok(d);
    }

    @DeleteMapping("/api/admin/dicts/{id}")
    public ApiResult<Void> deleteTenantDict(@PathVariable Long id) {
        tenantDictMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/api/platform/dicts")
    public ApiResult<List<Dict>> globalDicts(@RequestParam(required = false) String type) {
        AuthGuard.requirePlatform();
        LambdaQueryWrapper<Dict> w = new LambdaQueryWrapper<Dict>().orderByAsc(Dict::getType).orderByAsc(Dict::getSort);
        if (type != null && !type.isBlank()) w.eq(Dict::getType, type);
        return ApiResult.ok(dictMapper.selectList(w));
    }

    @PostMapping("/api/platform/dicts")
    public ApiResult<Dict> addGlobal(@RequestBody Dict dict) {
        AuthGuard.requirePlatform();
        dict.setId(null);
        dictMapper.insert(dict);
        return ApiResult.ok(dict);
    }

    @PutMapping("/api/platform/dicts/{id}")
    public ApiResult<Dict> updateGlobal(@PathVariable Long id, @RequestBody Dict dict) {
        AuthGuard.requirePlatform();
        dict.setId(id);
        dictMapper.updateById(dict);
        return ApiResult.ok(dictMapper.selectById(id));
    }

    @DeleteMapping("/api/platform/dicts/{id}")
    public ApiResult<Void> deleteGlobal(@PathVariable Long id) {
        AuthGuard.requirePlatform();
        dictMapper.deleteById(id);
        return ApiResult.ok();
    }
}
