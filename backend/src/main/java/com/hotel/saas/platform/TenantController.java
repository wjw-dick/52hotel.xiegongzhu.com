package com.hotel.saas.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class TenantController {
    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;
    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;
    private final MemMemberMapper memberMapper;
    private final PasswordEncoder encoder;

    @Data
    public static class TenantReq {
        private Long id;
        private String code;
        private String name;
        private String type;
        private String status;
        private String contact;
        private String phone;
        private LocalDate openTime;
        private LocalDate expireTime;
        private Integer maxUsers;
        private Integer maxRooms;
        private String remark;
        private String adminUsername;
        private String adminPassword;
    }

    @GetMapping
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String status) {
        AuthGuard.requirePlatform();
        LambdaQueryWrapper<Tenant> w = new LambdaQueryWrapper<Tenant>().orderByDesc(Tenant::getId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(Tenant::getName, keyword).or().like(Tenant::getCode, keyword));
        }
        if (status != null && !status.isBlank()) w.eq(Tenant::getStatus, status);
        Page<Tenant> p = tenantMapper.selectPage(Pages.of(page, size), w);
        List<Map<String, Object>> rows = p.getRecords().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("code", t.getCode());
            m.put("name", t.getName());
            m.put("type", t.getType());
            m.put("status", t.getStatus());
            m.put("contact", t.getContact());
            m.put("phone", t.getPhone());
            m.put("openTime", t.getOpenTime());
            m.put("expireTime", t.getExpireTime());
            m.put("maxUsers", t.getMaxUsers());
            m.put("maxRooms", t.getMaxRooms());
            m.put("roomCount", roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, t.getId())));
            m.put("userCount", tenantUserMapper.selectCount(new LambdaQueryWrapper<TenantUser>().eq(TenantUser::getTenantId, t.getId())));
            m.put("orderCount", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getTenantId, t.getId())));
            m.put("memberCount", memberMapper.selectCount(new LambdaQueryWrapper<MemMember>().eq(MemMember::getTenantId, t.getId())));
            m.put("expiring", t.getExpireTime() != null && t.getExpireTime().isBefore(LocalDate.now().plusDays(30)));
            return m;
        }).toList();
        return ApiResult.ok(new PageResult<>(rows, p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PostMapping
    @Transactional
    public ApiResult<Tenant> create(@RequestBody TenantReq req) {
        AuthGuard.requirePlatform();
        if (req.getCode() == null || req.getCode().isBlank() || req.getName() == null) {
            throw new BizException("租户编码与名称不能为空");
        }
        Long exist = tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, req.getCode()));
        if (exist != null && exist > 0) throw new BizException("租户编码已存在");
        Tenant t = new Tenant();
        t.setCode(req.getCode().toUpperCase());
        t.setName(req.getName());
        t.setType(req.getType() == null ? "HOTEL" : req.getType());
        t.setStatus(req.getStatus() == null ? "TRIAL" : req.getStatus());
        t.setContact(req.getContact());
        t.setPhone(req.getPhone());
        t.setOpenTime(req.getOpenTime() == null ? LocalDate.now() : req.getOpenTime());
        t.setExpireTime(req.getExpireTime() == null ? LocalDate.now().plusMonths(1) : req.getExpireTime());
        t.setMaxUsers(req.getMaxUsers() == null ? 20 : req.getMaxUsers());
        t.setMaxRooms(req.getMaxRooms() == null ? 100 : req.getMaxRooms());
        t.setRemark(req.getRemark());
        tenantMapper.insert(t);

        TenantUser admin = new TenantUser();
        admin.setTenantId(t.getId());
        admin.setUsername(req.getAdminUsername() == null ? "admin" : req.getAdminUsername());
        admin.setPassword(encoder.encode(req.getAdminPassword() == null ? "admin123" : req.getAdminPassword()));
        admin.setRealName(t.getName() + "管理员");
        admin.setRoleCode("TENANT_ADMIN");
        admin.setStatus(1);
        tenantUserMapper.insert(admin);
        return ApiResult.ok(t);
    }

    @PutMapping("/{id}")
    public ApiResult<Tenant> update(@PathVariable Long id, @RequestBody TenantReq req) {
        AuthGuard.requirePlatform();
        Tenant t = tenantMapper.selectById(id);
        if (t == null) throw new BizException(404, "租户不存在");
        if (req.getName() != null) t.setName(req.getName());
        if (req.getType() != null) t.setType(req.getType());
        if (req.getContact() != null) t.setContact(req.getContact());
        if (req.getPhone() != null) t.setPhone(req.getPhone());
        if (req.getExpireTime() != null) t.setExpireTime(req.getExpireTime());
        if (req.getMaxUsers() != null) t.setMaxUsers(req.getMaxUsers());
        if (req.getMaxRooms() != null) t.setMaxRooms(req.getMaxRooms());
        if (req.getRemark() != null) t.setRemark(req.getRemark());
        tenantMapper.updateById(t);
        return ApiResult.ok(t);
    }

    @PostMapping("/{id}/status")
    public ApiResult<Tenant> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requirePlatform();
        Tenant t = tenantMapper.selectById(id);
        if (t == null) throw new BizException(404, "租户不存在");
        String status = body.get("status");
        if (!Set.of("TRIAL", "NORMAL", "DISABLED").contains(status)) throw new BizException("状态不合法");
        t.setStatus(status);
        tenantMapper.updateById(t);
        return ApiResult.ok(t);
    }

    @PostMapping("/{id}/renew")
    public ApiResult<Tenant> renew(@PathVariable Long id, @RequestBody(required = false) Map<String, Integer> body) {
        AuthGuard.requirePlatform();
        Tenant t = tenantMapper.selectById(id);
        if (t == null) throw new BizException(404, "租户不存在");
        int months = body == null || body.get("months") == null ? 12 : body.get("months");
        LocalDate base = t.getExpireTime() == null || t.getExpireTime().isBefore(LocalDate.now())
                ? LocalDate.now() : t.getExpireTime();
        t.setExpireTime(base.plusMonths(months));
        t.setStatus("NORMAL");
        tenantMapper.updateById(t);
        return ApiResult.ok(t);
    }

    @GetMapping("/options")
    public ApiResult<List<Map<String, Object>>> options() {
        AuthGuard.requirePlatform();
        List<Tenant> list = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getId));
        return ApiResult.ok(list.stream().map(t -> Map.<String, Object>of(
                "id", t.getId(), "code", t.getCode(), "name", t.getName(), "status", t.getStatus())).toList());
    }
}
