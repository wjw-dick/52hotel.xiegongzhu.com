package com.hotel.saas.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.BizException;
import com.hotel.saas.common.JwtUtil;
import com.hotel.saas.common.TenantContext;
import com.hotel.saas.domain.MemMember;
import com.hotel.saas.domain.PlatformUser;
import com.hotel.saas.domain.Tenant;
import com.hotel.saas.domain.TenantUser;
import com.hotel.saas.mapper.MemMemberMapper;
import com.hotel.saas.mapper.PlatformUserMapper;
import com.hotel.saas.mapper.TenantMapper;
import com.hotel.saas.mapper.TenantUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PlatformUserMapper platformUserMapper;
    private final TenantUserMapper tenantUserMapper;
    private final TenantMapper tenantMapper;
    private final MemMemberMapper memberMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password, String tenantCode) {
        if (username == null || password == null) {
            throw new BizException("请输入账号和密码");
        }
        TenantContext.User user;
        Map<String, Object> profile = new LinkedHashMap<>();
        if (tenantCode == null || tenantCode.isBlank()) {
            PlatformUser pu = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                    .eq(PlatformUser::getUsername, username).last("limit 1"));
            if (pu == null || pu.getStatus() != 1 || !encoder.matches(password, pu.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            user = new TenantContext.User(pu.getId(), pu.getUsername(), 0L, "PLATFORM",
                    pu.getRoleCode(), true, null);
            profile.put("realName", pu.getRealName());
            profile.put("tenantName", "平台运营中心");
        } else {
            Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                    .eq(Tenant::getCode, tenantCode).last("limit 1"));
            if (tenant == null) {
                throw new BizException(404, "租户不存在");
            }
            checkTenant(tenant);
            TenantContext.set(new TenantContext.User(null, username, tenant.getId(), tenant.getCode(),
                    "TENANT_ADMIN", false, null));
            TenantUser tu = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                    .eq(TenantUser::getTenantId, tenant.getId())
                    .eq(TenantUser::getUsername, username).last("limit 1"));
            if (tu == null || tu.getStatus() != 1 || !encoder.matches(password, tu.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            user = new TenantContext.User(tu.getId(), tu.getUsername(), tenant.getId(), tenant.getCode(),
                    tu.getRoleCode(), false, tu.getStoreId());
            profile.put("realName", tu.getRealName());
            profile.put("tenantName", tenant.getName());
            profile.put("storeId", tu.getStoreId());
        }
        String token = jwtUtil.create(user);
        profile.put("token", token);
        profile.put("userId", user.userId());
        profile.put("username", user.username());
        profile.put("role", user.role());
        profile.put("tenantId", user.tenantId());
        profile.put("tenantCode", user.tenantCode());
        profile.put("platform", user.platform());
        return profile;
    }

    public void checkTenant(Tenant tenant) {
        if (!"NORMAL".equals(tenant.getStatus()) && !"TRIAL".equals(tenant.getStatus())) {
            throw new BizException(403, "租户已停用，请联系平台");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDate.now())) {
            throw new BizException(403, "租户已到期，请联系平台续期");
        }
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        TenantContext.User u = TenantContext.get();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        if (u.platform()) {
            PlatformUser pu = platformUserMapper.selectById(u.userId());
            if (pu == null || !encoder.matches(oldPassword, pu.getPassword())) {
                throw new BizException("原密码不正确");
            }
            pu.setPassword(encoder.encode(newPassword));
            platformUserMapper.updateById(pu);
        } else if ("MEMBER".equals(u.role())) {
            MemMember m = memberMapper.selectById(u.userId());
            if (m == null) {
                throw new BizException("会员不存在");
            }
            m.setPhone(m.getPhone());
            // 会员默认以手机号后 6 位作为初始密码，此处仅记录提示，统一由短信验证码重置
            throw new BizException("会员请通过手机号验证码重置密码");
        } else {
            TenantUser tu = tenantUserMapper.selectById(u.userId());
            if (tu == null || !encoder.matches(oldPassword, tu.getPassword())) {
                throw new BizException("原密码不正确");
            }
            tu.setPassword(encoder.encode(newPassword));
            tenantUserMapper.updateById(tu);
        }
    }

    /** 小程序/公众号/App 统一登录：demo 环境使用 openid 模拟，真实环境替换 code2Session */
    public Map<String, Object> wechatLogin(String tenantCode, String openId, String unionId, String nickname, String phone) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, tenantCode).last("limit 1"));
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        checkTenant(tenant);
        TenantContext.set(new TenantContext.User(null, "member", tenant.getId(), tenant.getCode(),
                "MEMBER", false, null));
        MemMember member = memberMapper.selectOne(new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getTenantId, tenant.getId())
                .and(w -> w.eq(MemMember::getOpenId, openId).or().eq(MemMember::getUnionId, unionId))
                .last("limit 1"));
        if (member == null) {
            member = new MemMember();
            member.setTenantId(tenant.getId());
            member.setOpenId(openId);
            member.setUnionId(unionId == null ? openId : unionId);
            member.setName(nickname == null ? "微信用户" : nickname);
            member.setPhone(phone);
            member.setPoints(0);
            member.setBalance(java.math.BigDecimal.ZERO);
            member.setTotalAmount(java.math.BigDecimal.ZERO);
            member.setStatus(1);
            memberMapper.insert(member);
        }
        TenantContext.User u = new TenantContext.User(member.getId(), "member_" + member.getId(),
                tenant.getId(), tenant.getCode(), "MEMBER", false, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.create(u));
        result.put("member", member);
        result.put("tenantId", tenant.getId());
        result.put("tenantCode", tenant.getCode());
        result.put("tenantName", tenant.getName());
        return result;
    }

    public Map<String, Object> profile() {
        TenantContext.User u = TenantContext.get();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.userId());
        m.put("username", u.username());
        m.put("role", u.role());
        m.put("tenantId", u.tenantId());
        m.put("tenantCode", u.tenantCode());
        m.put("platform", u.platform());
        if (u.platform()) {
            PlatformUser pu = platformUserMapper.selectById(u.userId());
            m.put("realName", pu == null ? "平台管理员" : pu.getRealName());
            m.put("tenantName", "平台运营中心");
        } else if ("MEMBER".equals(u.role())) {
            MemMember member = memberMapper.selectById(u.userId());
            m.put("member", member);
            Tenant t = tenantMapper.selectById(u.tenantId());
            m.put("tenantName", t == null ? "" : t.getName());
        } else {
            TenantUser tu = tenantUserMapper.selectById(u.userId());
            m.put("realName", tu == null ? u.username() : tu.getRealName());
            m.put("storeId", tu == null ? null : tu.getStoreId());
            Tenant t = tenantMapper.selectById(u.tenantId());
            m.put("tenantName", t == null ? "" : t.getName());
        }
        return m;
    }
}
