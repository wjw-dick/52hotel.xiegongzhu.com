package com.hotel.saas.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.booking.OrderService;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberPortalController {
    private final MemMemberMapper memberMapper;
    private final MemLevelMapper levelMapper;
    private final MemPointLogMapper pointMapper;
    private final MemCouponMapper couponMapper;
    private final OrderMapper orderMapper;
    private final NotificationMapper notificationMapper;
    private final OrderService orderService;
    private final PasswordEncoder encoder;

    private MemMember current() {
        TenantContext.User u = TenantContext.get();
        if (u == null || !"MEMBER".equals(u.role())) throw new BizException(401, "请先登录会员账号");
        MemMember m = memberMapper.selectById(u.userId());
        if (m == null) throw new BizException(404, "会员不存在");
        return m;
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        MemMember m = current();
        MemLevel level = m.getLevelId() == null ? null : levelMapper.selectById(m.getLevelId());
        List<MemCoupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<MemCoupon>()
                .eq(MemCoupon::getMemberId, m.getId()).eq(MemCoupon::getStatus, "UNUSED"));
        Map<String, Object> result = new LinkedHashMap<>();
        m.setPassword(null);
        result.put("member", m);
        result.put("level", level);
        result.put("couponCount", coupons.size());
        result.put("orderCount", orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getMemberId, m.getId())));
        return ApiResult.ok(result);
    }

    @GetMapping("/orders")
    public ApiResult<List<Order>> orders() {
        MemMember m = current();
        return ApiResult.ok(orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMemberId, m.getId()).orderByDesc(Order::getId)));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        MemMember m = current();
        Order o = orderMapper.selectById(id);
        if (o == null || !Objects.equals(o.getMemberId(), m.getId())) throw new BizException(403, "无权操作该订单");
        orderService.cancel(id, body == null ? "会员自助取消" : body.getOrDefault("reason", "会员自助取消"));
        return ApiResult.ok();
    }

    @GetMapping("/points")
    public ApiResult<List<MemPointLog>> points() {
        MemMember m = current();
        return ApiResult.ok(pointMapper.selectList(new LambdaQueryWrapper<MemPointLog>()
                .eq(MemPointLog::getMemberId, m.getId()).orderByDesc(MemPointLog::getId)));
    }

    @GetMapping("/coupons")
    public ApiResult<List<MemCoupon>> coupons() {
        MemMember m = current();
        return ApiResult.ok(couponMapper.selectList(new LambdaQueryWrapper<MemCoupon>()
                .eq(MemCoupon::getMemberId, m.getId()).orderByDesc(MemCoupon::getId)));
    }

    @GetMapping("/notifications")
    public ApiResult<List<Notification>> notifications() {
        Long tid = TenantContext.tenantId();
        return ApiResult.ok(notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTenantId, tid).orderByDesc(Notification::getId).last("limit 50")));
    }

    @Data
    public static class ProfileReq {
        private String name;
        private String phone;
        private String gender;
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody ProfileReq req) {
        MemMember m = current();
        if (req.getName() != null) m.setName(req.getName());
        if (req.getPhone() != null) m.setPhone(req.getPhone());
        if (req.getGender() != null) m.setGender(req.getGender());
        memberMapper.updateById(m);
        return ApiResult.ok();
    }

    @Data
    public static class PasswordReq {
        private String code;
        private String newPassword;
    }

    /** C 端修改密码：演示环境使用手机号后 6 位作为短信验证码。 */
    @PostMapping("/password")
    public ApiResult<Void> password(@RequestBody PasswordReq req) {
        MemMember m = current();
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        String phone = m.getPhone();
        if (phone == null || phone.length() < 6 || req.getCode() == null
                || !phone.substring(phone.length() - 6).equals(req.getCode())) {
            throw new BizException("验证码不正确");
        }
        m.setPassword(encoder.encode(req.getNewPassword()));
        memberMapper.updateById(m);
        return ApiResult.ok();
    }
}
