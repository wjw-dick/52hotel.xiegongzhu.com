package com.hotel.saas.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemMemberMapper memberMapper;
    private final MemLevelMapper levelMapper;
    private final MemPointLogMapper pointLogMapper;
    private final MemCouponMapper couponMapper;
    private final OrderMapper orderMapper;
    private final CheckinMapper checkinMapper;

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Long levelId) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<MemMember> w = new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getTenantId, tid).orderByDesc(MemMember::getId);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(MemMember::getName, keyword).or().like(MemMember::getPhone, keyword)
                    .or().like(MemMember::getUnionId, keyword));
        }
        if (levelId != null) w.eq(MemMember::getLevelId, levelId);
        Map<Long, String> levels = levelMapper.selectList(new LambdaQueryWrapper<MemLevel>()
                        .eq(MemLevel::getTenantId, tid)).stream()
                .collect(Collectors.toMap(MemLevel::getId, MemLevel::getName));
        return ApiResult.ok(memberMapper.selectList(w).stream().map(m -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("name", m.getName());
            row.put("phone", maskPhone(m.getPhone()));
            row.put("rawPhone", m.getPhone());
            row.put("gender", m.getGender());
            row.put("unionId", m.getUnionId());
            row.put("levelId", m.getLevelId());
            row.put("levelName", levels.getOrDefault(m.getLevelId(), "普通会员"));
            row.put("points", m.getPoints());
            row.put("balance", m.getBalance());
            row.put("totalAmount", m.getTotalAmount());
            row.put("tags", m.getTags());
            row.put("status", m.getStatus());
            row.put("createTime", m.getCreateTime());
            return row;
        }).toList());
    }

    @GetMapping("/levels")
    public ApiResult<List<MemLevel>> levels() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(levelMapper.selectList(new LambdaQueryWrapper<MemLevel>()
                .eq(MemLevel::getTenantId, tid).orderByAsc(MemLevel::getSort)));
    }

    @Data
    public static class MemberReq {
        private Long id;
        private String name;
        private String phone;
        private String gender;
        private Long levelId;
        private String tags;
        private Integer status;
    }

    @PostMapping
    public ApiResult<MemMember> create(@RequestBody MemberReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getName() == null || req.getPhone() == null) throw new BizException("姓名与手机号不能为空");
        Long dup = memberMapper.selectCount(new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getTenantId, tid).eq(MemMember::getPhone, req.getPhone()));
        if (dup != null && dup > 0) throw new BizException("该手机号已是会员");
        MemMember m = new MemMember();
        m.setTenantId(tid);
        m.setName(req.getName());
        m.setPhone(req.getPhone());
        m.setGender(req.getGender());
        m.setLevelId(req.getLevelId());
        m.setTags(req.getTags());
        m.setPoints(0);
        m.setBalance(BigDecimal.ZERO);
        m.setTotalAmount(BigDecimal.ZERO);
        m.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        memberMapper.insert(m);
        return ApiResult.ok(m);
    }

    @PutMapping("/{id}")
    public ApiResult<MemMember> update(@PathVariable Long id, @RequestBody MemberReq req) {
        MemMember m = memberMapper.selectById(id);
        if (m == null || !m.getTenantId().equals(TenantScope.requiredTenantId())) throw new BizException(404, "会员不存在");
        if (req.getName() != null) m.setName(req.getName());
        if (req.getPhone() != null) m.setPhone(req.getPhone());
        if (req.getGender() != null) m.setGender(req.getGender());
        if (req.getLevelId() != null) m.setLevelId(req.getLevelId());
        if (req.getTags() != null) m.setTags(req.getTags());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        memberMapper.updateById(m);
        return ApiResult.ok(m);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        memberMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/detail")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Long tid = TenantScope.requiredTenantId();
        MemMember m = memberMapper.selectById(id);
        if (m == null) throw new BizException(404, "会员不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        m.setPhone(maskPhone(m.getPhone()));
        result.put("member", m);
        result.put("orders", orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getMemberId, id).orderByDesc(Order::getId).last("limit 20")));
        result.put("points", pointLogMapper.selectList(new LambdaQueryWrapper<MemPointLog>()
                .eq(MemPointLog::getMemberId, id).orderByDesc(MemPointLog::getId).last("limit 30")));
        result.put("coupons", couponMapper.selectList(new LambdaQueryWrapper<MemCoupon>()
                .eq(MemCoupon::getMemberId, id).orderByDesc(MemCoupon::getId)));
        return ApiResult.ok(result);
    }

    @Data
    public static class PointReq {
        private Integer changeValue;
        private String reason;
    }

    @PostMapping("/{id}/points")
    public ApiResult<Void> points(@PathVariable Long id, @RequestBody PointReq req) {
        MemMember m = memberMapper.selectById(id);
        if (m == null) throw new BizException(404, "会员不存在");
        if (req.getChangeValue() == null || req.getChangeValue() == 0) throw new BizException("积分变动值不能为 0");
        int newPoints = (m.getPoints() == null ? 0 : m.getPoints()) + req.getChangeValue();
        if (newPoints < 0) throw new BizException("积分余额不足");
        m.setPoints(newPoints);
        memberMapper.updateById(m);
        MemPointLog log = new MemPointLog();
        log.setTenantId(m.getTenantId());
        log.setMemberId(id);
        log.setChangeValue(req.getChangeValue());
        log.setReason(req.getReason() == null ? "人工调整" : req.getReason());
        pointLogMapper.insert(log);
        return ApiResult.ok();
    }

    @Data
    public static class CouponReq {
        private String name;
        private BigDecimal amount;
        private String expireDate;
    }

    @PostMapping("/{id}/coupons")
    public ApiResult<MemCoupon> coupon(@PathVariable Long id, @RequestBody CouponReq req) {
        MemMember m = memberMapper.selectById(id);
        if (m == null) throw new BizException(404, "会员不存在");
        MemCoupon c = new MemCoupon();
        c.setTenantId(m.getTenantId());
        c.setMemberId(id);
        c.setName(req.getName());
        c.setAmount(req.getAmount() == null ? BigDecimal.ZERO : req.getAmount());
        c.setStatus("UNUSED");
        if (req.getExpireDate() != null) c.setExpireDate(java.time.LocalDate.parse(req.getExpireDate()));
        couponMapper.insert(c);
        return ApiResult.ok(c);
    }

    @PostMapping("/levels/refresh")
    public ApiResult<Map<String, Object>> refreshLevels() {
        Long tid = TenantScope.requiredTenantId();
        List<MemLevel> levels = levelMapper.selectList(new LambdaQueryWrapper<MemLevel>()
                .eq(MemLevel::getTenantId, tid).orderByDesc(MemLevel::getMinAmount));
        List<MemMember> members = memberMapper.selectList(new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getTenantId, tid));
        int upgraded = 0;
        for (MemMember m : members) {
            MemLevel target = levels.stream()
                    .filter(l -> m.getTotalAmount() != null && m.getTotalAmount().compareTo(l.getMinAmount()) >= 0)
                    .findFirst().orElse(null);
            if (target != null && !target.getId().equals(m.getLevelId())) {
                m.setLevelId(target.getId());
                memberMapper.updateById(m);
                upgraded++;
            }
        }
        return ApiResult.ok(Map.of("upgraded", upgraded, "total", members.size()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
