package com.hotel.saas.report;

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
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;
    private final CheckinMapper checkinMapper;
    private final MemMemberMapper memberMapper;
    private final FinSettlementMapper settlementMapper;
    private final HkTaskMapper taskMapper;
    private final TenantMapper tenantMapper;

    @GetMapping
    public ApiResult<Map<String, Object>> dashboard() {
        Long tid = TenantScope.requiredTenantId();
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
        long total = rooms.size();
        long occupied = rooms.stream().filter(r -> "OCCUPIED".equals(r.getStatus())).count();
        long dirty = rooms.stream().filter(r -> "DIRTY".equals(r.getStatus())).count();
        long vacant = rooms.stream().filter(r -> "VACANT".equals(r.getStatus())).count();
        long repair = rooms.stream().filter(r -> "REPAIR".equals(r.getStatus())).count();
        long todayArrivals = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getCheckIn, LocalDate.now())
                .in(Order::getStatus, "CONFIRMED", "PENDING"));
        long todayDepartures = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getCheckOut, LocalDate.now())
                .eq(Order::getStatus, "CHECKED_IN"));
        long inHouse = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getTenantId, tid).eq(Checkin::getStatus, "IN_HOUSE"));
        long pendingTasks = taskMapper.selectCount(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getTenantId, tid).ne(HkTask::getStatus, "DONE"));

        List<FinSettlement> settlements = settlementMapper.selectList(new LambdaQueryWrapper<FinSettlement>()
                .eq(FinSettlement::getTenantId, tid));
        BigDecimal todayRevenue = settlements.stream()
                .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(LocalDate.now()))
                .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthRevenue = settlements.stream()
                .filter(s -> s.getCreateTime() != null &&
                        s.getCreateTime().toLocalDate().getMonth() == LocalDate.now().getMonth()
                        && s.getCreateTime().toLocalDate().getYear() == LocalDate.now().getYear())
                .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate date = LocalDate.now().minusDays(d);
            BigDecimal dayRevenue = settlements.stream()
                    .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(date))
                    .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getTenantId, tid).eq(Order::getCheckIn, date));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());
            point.put("revenue", dayRevenue);
            point.put("orders", dayOrders);
            trend.add(point);
        }

        List<Map<String, Object>> arrivals = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .eq(Order::getTenantId, tid).eq(Order::getCheckIn, LocalDate.now())
                        .orderByAsc(Order::getId).last("limit 6")).stream()
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId());
                    m.put("orderNo", o.getOrderNo());
                    m.put("guestName", o.getGuestName());
                    m.put("guestPhone", o.getGuestPhone());
                    m.put("status", o.getStatus());
                    m.put("amount", o.getAmount());
                    return m;
                }).toList();

        Tenant tenant = tenantMapper.selectById(tid);
        Map<String, Object> statusCount = new LinkedHashMap<>();
        statusCount.put("VACANT", vacant);
        statusCount.put("OCCUPIED", occupied);
        statusCount.put("DIRTY", dirty);
        statusCount.put("REPAIR", repair);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantName", tenant == null ? "" : tenant.getName());
        result.put("expireTime", tenant == null ? null : tenant.getExpireTime());
        result.put("roomCount", total);
        result.put("occupied", occupied);
        result.put("occupancy", total == 0 ? 0 : Math.round(occupied * 1000.0 / total) / 10.0);
        result.put("todayArrivals", todayArrivals);
        result.put("todayDepartures", todayDepartures);
        result.put("inHouse", inHouse);
        result.put("dirty", dirty);
        result.put("pendingTasks", pendingTasks);
        result.put("todayRevenue", todayRevenue);
        result.put("monthRevenue", monthRevenue);
        result.put("memberCount", memberMapper.selectCount(new LambdaQueryWrapper<MemMember>().eq(MemMember::getTenantId, tid)));
        result.put("trend", trend);
        result.put("statusCount", statusCount);
        result.put("arrivals", arrivals);
        return ApiResult.ok(result);
    }
}
