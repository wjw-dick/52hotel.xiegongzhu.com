package com.hotel.saas.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final TenantUserMapper userMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final AuditLogMapper auditMapper;
    private final PasswordEncoder encoder;

    @GetMapping("/api/admin/users")
    public ApiResult<List<Map<String, Object>>> users() {
        Long tid = TenantScope.requiredTenantId();
        List<TenantUser> users = userMapper.selectList(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tid).orderByAsc(TenantUser::getId));
        Map<String, String> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .eq(Role::getTenantId, tid)).stream()
                .collect(java.util.stream.Collectors.toMap(Role::getCode, Role::getName));
        return ApiResult.ok(users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("roleCode", u.getRoleCode());
            m.put("roleName", roles.getOrDefault(u.getRoleCode(), u.getRoleCode()));
            m.put("storeId", u.getStoreId());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            return m;
        }).toList());
    }

    @GetMapping("/api/admin/roles")
    public ApiResult<List<Role>> roles() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getTenantId, tid)));
    }

    @Data
    public static class UserReq {
        private Long id;
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String roleCode;
        private Long storeId;
        private Integer status;
    }

    @PostMapping("/api/admin/users")
    public ApiResult<TenantUser> create(@RequestBody UserReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getUsername() == null || req.getPassword() == null) throw new BizException("账号与密码不能为空");
        if (req.getPassword().length() < 6) throw new BizException("密码至少 6 位");
        Tenant tenant = tenantMapper.selectById(tid);
        long count = userMapper.selectCount(new LambdaQueryWrapper<TenantUser>().eq(TenantUser::getTenantId, tid));
        if (tenant != null && tenant.getMaxUsers() != null && count >= tenant.getMaxUsers()) {
            throw new BizException("租户用户数已达配额上限");
        }
        Long dup = userMapper.selectCount(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tid).eq(TenantUser::getUsername, req.getUsername()));
        if (dup != null && dup > 0) throw new BizException("账号已存在");
        TenantUser u = new TenantUser();
        u.setTenantId(tid);
        u.setUsername(req.getUsername());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRealName(req.getRealName());
        u.setPhone(req.getPhone());
        u.setRoleCode(req.getRoleCode() == null ? "FRONT_DESK" : req.getRoleCode());
        u.setStoreId(req.getStoreId());
        u.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(u);
        return ApiResult.ok(u);
    }

    @PutMapping("/api/admin/users/{id}")
    public ApiResult<TenantUser> update(@PathVariable Long id, @RequestBody UserReq req) {
        TenantUser u = userMapper.selectById(id);
        if (u == null || !u.getTenantId().equals(TenantScope.requiredTenantId())) throw new BizException(404, "用户不存在");
        if (req.getRealName() != null) u.setRealName(req.getRealName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getRoleCode() != null) u.setRoleCode(req.getRoleCode());
        if (req.getStoreId() != null) u.setStoreId(req.getStoreId());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 6) throw new BizException("密码至少 6 位");
            u.setPassword(encoder.encode(req.getPassword()));
        }
        userMapper.updateById(u);
        return ApiResult.ok(u);
    }

    @PostMapping("/api/admin/users/{id}/reset-password")
    public ApiResult<Void> reset(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TenantUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        String pwd = body.getOrDefault("password", "123456");
        if (pwd.length() < 6) throw new BizException("密码至少 6 位");
        u.setPassword(encoder.encode(pwd));
        userMapper.updateById(u);
        return ApiResult.ok();
    }

    @DeleteMapping("/api/admin/users/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        if (id.equals(TenantContext.userId())) throw new BizException("不能删除当前登录账号");
        userMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/api/admin/audits")
    public ApiResult<PageResult<AuditLog>> audits(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String module,
                                                  @RequestParam(required = false) String keyword) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTenantId, tid).orderByDesc(AuditLog::getId);
        if (module != null && !module.isBlank()) w.eq(AuditLog::getModule, module);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(AuditLog::getUsername, keyword).or().like(AuditLog::getAction, keyword)
                    .or().like(AuditLog::getTarget, keyword));
        }
        Page<AuditLog> p = auditMapper.selectPage(Pages.of(page, size), w);
        return ApiResult.ok(Pages.result(p));
    }

    @GetMapping("/api/platform/audits")
    public ApiResult<PageResult<AuditLog>> platformAudits(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long tenantId,
                                                          @RequestParam(required = false) String keyword) {
        AuthGuard.requirePlatform();
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getId);
        if (tenantId != null) w.eq(AuditLog::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(AuditLog::getUsername, keyword).or().like(AuditLog::getAction, keyword));
        }
        return ApiResult.ok(Pages.result(auditMapper.selectPage(Pages.of(page, size), w)));
    }
}
