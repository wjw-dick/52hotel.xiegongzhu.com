package com.hotel.saas.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {
    private final OrderMapper orderMapper;
    private final FinSettlementMapper settlementMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final StoreMapper storeMapper;
    private final MemMemberMapper memberMapper;

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary(@RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate) {
        Long tid = TenantScope.requiredTenantId();
        LocalDate end = endDate == null || endDate.isBlank() ? LocalDate.now() : LocalDate.parse(endDate);
        LocalDate start = startDate == null || startDate.isBlank() ? end.minusDays(29) : LocalDate.parse(startDate);
        if (start.isAfter(end)) throw new BizException("开始日期不能晚于结束日期");

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).between(Order::getCheckIn, start, end)
                .ne(Order::getStatus, "CANCELLED"));
        List<FinSettlement> settlements = settlementMapper.selectList(new LambdaQueryWrapper<FinSettlement>()
                .eq(FinSettlement::getTenantId, tid)
                .ge(FinSettlement::getCreateTime, start + " 00:00:00")
                .le(FinSettlement::getCreateTime, end + " 23:59:59"));
        BigDecimal revenue = settlements.stream().map(FinSettlement::getTotal)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
        long roomNights = orders.stream().mapToLong(o -> o.getNights() == null ? 1 : o.getNights()).sum();
        long days = end.toEpochDay() - start.toEpochDay() + 1;
        double occupancy = roomCount == 0 ? 0 : Math.min(100, roomNights * 100.0 / (roomCount * days));
        BigDecimal adr = roomNights == 0 ? BigDecimal.ZERO : revenue.divide(new BigDecimal(roomNights), 2, RoundingMode.HALF_UP);
        BigDecimal revPar = roomCount == 0 || days == 0 ? BigDecimal.ZERO
                : revenue.divide(new BigDecimal(roomCount * days), 2, RoundingMode.HALF_UP);

        Map<String, Long> source = orders.stream().collect(Collectors.groupingBy(
                o -> o.getSource() == null ? "WALK_IN" : o.getSource(), TreeMap::new, Collectors.counting()));

        Map<Long, String> typeNames = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, BigDecimal> typeRevenue = new LinkedHashMap<>();
        Map<Long, Long> typeOrders = new LinkedHashMap<>();
        for (Order o : orders) {
            typeOrders.merge(o.getRoomTypeId(), 1L, Long::sum);
            typeRevenue.merge(o.getRoomTypeId(), o.getAmount() == null ? BigDecimal.ZERO : o.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> roomTypeStats = typeRevenue.entrySet().stream().map(e -> Map.<String, Object>of(
                "roomTypeId", e.getKey(), "roomTypeName", typeNames.getOrDefault(e.getKey(), ""),
                "orders", typeOrders.getOrDefault(e.getKey(), 0L), "revenue", e.getValue())).toList();

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDate day = d;
            BigDecimal dayRevenue = settlements.stream()
                    .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(day))
                    .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayOrders = orders.stream().filter(o -> day.equals(o.getCheckIn())).count();
            trend.add(Map.of("date", day.toString(), "revenue", dayRevenue, "orders", dayOrders));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        result.put("revenue", revenue);
        result.put("orderCount", orders.size());
        result.put("roomNights", roomNights);
        result.put("roomCount", roomCount);
        result.put("occupancy", Math.round(occupancy * 10) / 10.0);
        result.put("adr", adr);
        result.put("revpar", revPar);
        result.put("memberCount", memberMapper.selectCount(new LambdaQueryWrapper<MemMember>().eq(MemMember::getTenantId, tid)));
        result.put("source", source);
        result.put("roomTypeStats", roomTypeStats);
        result.put("trend", trend);
        result.put("storeStats", storeStats(tid, orders));
        return ApiResult.ok(result);
    }

    private List<Map<String, Object>> storeStats(Long tid, List<Order> orders) {
        Map<Long, String> names = storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getTenantId, tid))
                .stream().collect(Collectors.toMap(Store::getId, Store::getName));
        Map<Long, BigDecimal> revenue = new LinkedHashMap<>();
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Order o : orders) {
            counts.merge(o.getStoreId(), 1L, Long::sum);
            revenue.merge(o.getStoreId(), o.getAmount() == null ? BigDecimal.ZERO : o.getAmount(), BigDecimal::add);
        }
        return revenue.entrySet().stream().map(e -> Map.<String, Object>of(
                "storeId", e.getKey(), "storeName", names.getOrDefault(e.getKey(), ""),
                "orders", counts.getOrDefault(e.getKey(), 0L), "revenue", e.getValue())).toList();
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws Exception {
        Long tid = TenantScope.requiredTenantId();
        LocalDate end = endDate == null || endDate.isBlank() ? LocalDate.now() : LocalDate.parse(endDate);
        LocalDate start = startDate == null || startDate.isBlank() ? end.minusDays(29) : LocalDate.parse(startDate);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).between(Order::getCheckIn, start, end));
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=report.csv");
        response.getWriter().write('\ufeff');
        var writer = response.getWriter();
        writer.println("订单号,入住人,手机号,入住,离店,间夜,金额,状态,来源");
        for (Order o : orders) {
            writer.println(o.getOrderNo() + "," + o.getGuestName() + "," + o.getGuestPhone() + ","
                    + o.getCheckIn() + "," + o.getCheckOut() + "," + o.getNights() + "," + o.getAmount() + ","
                    + o.getStatus() + "," + o.getSource());
        }
        writer.flush();
    }
}
