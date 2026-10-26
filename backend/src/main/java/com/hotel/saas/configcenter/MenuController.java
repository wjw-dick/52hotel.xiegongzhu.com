package com.hotel.saas.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.MenuConfig;
import com.hotel.saas.domain.Tenant;
import com.hotel.saas.mapper.MenuConfigMapper;
import com.hotel.saas.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MenuController {
    private final MenuConfigMapper menuMapper;
    private final TenantMapper tenantMapper;

    @GetMapping("/api/admin/menus")
    public ApiResult<List<MenuConfig>> list(@RequestParam(required = false, defaultValue = "ADMIN") String platform) {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<MenuConfig>()
                .eq(MenuConfig::getTenantId, tid).eq(MenuConfig::getPlatform, platform)
                .orderByAsc(MenuConfig::getSort)));
    }

    @PostMapping("/api/admin/menus")
    public ApiResult<MenuConfig> create(@RequestBody MenuConfig menu) {
        menu.setId(null);
        menu.setTenantId(TenantScope.requiredTenantId());
        if (menu.getEnabled() == null) menu.setEnabled(1);
        if (menu.getSort() == null) menu.setSort(0);
        menuMapper.insert(menu);
        return ApiResult.ok(menu);
    }

    @PutMapping("/api/admin/menus/{id}")
    public ApiResult<MenuConfig> update(@PathVariable Long id, @RequestBody MenuConfig menu) {
        Long tid = TenantScope.requiredTenantId();
        MenuConfig db = menuMapper.selectOne(new LambdaQueryWrapper<MenuConfig>()
                .eq(MenuConfig::getId, id).eq(MenuConfig::getTenantId, tid));
        if (db == null) throw new BizException(404, "菜单不存在");
        menu.setId(id);
        menu.setTenantId(db.getTenantId());
        menuMapper.updateById(menu);
        return ApiResult.ok(menuMapper.selectById(id));
    }

    @DeleteMapping("/api/admin/menus/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long tid = TenantScope.requiredTenantId();
        menuMapper.delete(new LambdaQueryWrapper<MenuConfig>()
                .eq(MenuConfig::getId, id).eq(MenuConfig::getTenantId, tid));
        return ApiResult.ok();
    }

    @PutMapping("/api/admin/menus/sort")
    public ApiResult<Void> sort(@RequestBody List<MenuConfig> menus) {
        Long tid = TenantScope.requiredTenantId();
        for (MenuConfig m : menus) {
            if (m.getId() == null) continue;
            MenuConfig db = menuMapper.selectOne(new LambdaQueryWrapper<MenuConfig>()
                    .eq(MenuConfig::getId, m.getId()).eq(MenuConfig::getTenantId, tid));
            if (db != null) {
                db.setSort(m.getSort());
                menuMapper.updateById(db);
            }
        }
        return ApiResult.ok();
    }

    @GetMapping("/api/public/{tenantCode}/menus")
    public ApiResult<List<MenuConfig>> publicMenus(@PathVariable String tenantCode,
                                                   @RequestParam(defaultValue = "H5") String platform) {
        Tenant t = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, tenantCode).last("limit 1"));
        if (t == null) throw new BizException(404, "租户不存在");
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<MenuConfig>()
                .eq(MenuConfig::getTenantId, t.getId()).eq(MenuConfig::getPlatform, platform)
                .eq(MenuConfig::getEnabled, 1).orderByAsc(MenuConfig::getSort)));
    }
}
