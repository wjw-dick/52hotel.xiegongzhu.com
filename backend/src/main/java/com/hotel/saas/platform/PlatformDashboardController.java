package com.hotel.saas.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {
    private final TenantMapper tenantMapper;
    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;
    private final MemMemberMapper memberMapper;
    private final FinSettlementMapper settlementMapper;
    private final TenantUserMapper userMapper;

    @GetMapping
    public ApiResult<Map<String, Object>> dashboard() {
        AuthGuard.requirePlatform();
        List<Tenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getId));
        long totalRooms = roomMapper.selectCount(null);
        long occupied = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OCCUPIED"));
        long totalOrders = orderMapper.selectCount(null);
        long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getCheckIn, LocalDate.now()));
        long members = memberMapper.selectCount(null);
        List<FinSettlement> settlements = settlementMapper.selectList(null);
        BigDecimal revenue = settlements.stream().map(FinSettlement::getTotal)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthRevenue = settlements.stream()
                .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().withDayOfMonth(1)
                        .equals(LocalDate.now().withDayOfMonth(1)))
                .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> tenantRows = new ArrayList<>();
        for (Tenant t : tenants) {
            long rooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, t.getId()));
            long occ = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                    .eq(Room::getTenantId, t.getId()).eq(Room::getStatus, "OCCUPIED"));
            long orders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getTenantId, t.getId()));
            BigDecimal rev = settlementMapper.selectList(new LambdaQueryWrapper<FinSettlement>()
                            .eq(FinSettlement::getTenantId, t.getId())).stream()
                    .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", t.getId());
            row.put("code", t.getCode());
            row.put("name", t.getName());
            row.put("status", t.getStatus());
            row.put("expireTime", t.getExpireTime());
            row.put("rooms", rooms);
            row.put("occupied", occ);
            row.put("occupancy", rooms == 0 ? 0 : Math.round(occ * 1000.0 / rooms) / 10.0);
            row.put("orders", orders);
            row.put("revenue", rev);
            row.put("members", memberMapper.selectCount(new LambdaQueryWrapper<MemMember>().eq(MemMember::getTenantId, t.getId())));
            row.put("users", userMapper.selectCount(new LambdaQueryWrapper<TenantUser>().eq(TenantUser::getTenantId, t.getId())));
            tenantRows.add(row);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate date = LocalDate.now().minusDays(d);
            BigDecimal dayRevenue = settlements.stream()
                    .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(date))
                    .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getCheckIn, date));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());
            point.put("revenue", dayRevenue);
            point.put("orders", dayOrders);
            trend.add(point);
        }

        List<Map<String, Object>> alerts = tenants.stream()
                .filter(t -> t.getExpireTime() != null && !t.getExpireTime().isAfter(LocalDate.now().plusDays(30)))
                .map(t -> Map.<String, Object>of("tenantId", t.getId(), "name", t.getName(),
                        "expireTime", t.getExpireTime().toString(),
                        "days", t.getExpireTime().toEpochDay() - LocalDate.now().toEpochDay()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantCount", tenants.size());
        result.put("activeTenantCount", tenants.stream().filter(t -> "NORMAL".equals(t.getStatus())).count());
        result.put("roomCount", totalRooms);
        result.put("occupied", occupied);
        result.put("occupancy", totalRooms == 0 ? 0 : Math.round(occupied * 1000.0 / totalRooms) / 10.0);
        result.put("orderCount", totalOrders);
        result.put("todayOrders", todayOrders);
        result.put("memberCount", members);
        result.put("revenue", revenue);
        result.put("monthRevenue", monthRevenue);
        result.put("tenants", tenantRows);
        result.put("trend", trend);
        result.put("alerts", alerts);
        return ApiResult.ok(result);
    }
}
