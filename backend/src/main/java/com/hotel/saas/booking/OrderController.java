package com.hotel.saas.booking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final OrderGuestMapper guestMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CheckinMapper checkinMapper;
    private final StoreMapper storeMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinDepositMapper depositMapper;
    private final CryptoService crypto;

    @GetMapping
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false) Long storeId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String source,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String startDate,
                                                           @RequestParam(required = false) String endDate) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).orderByDesc(Order::getId);
        if (storeId != null) w.eq(Order::getStoreId, storeId);
        if (status != null && !status.isBlank()) w.eq(Order::getStatus, status);
        if (source != null && !source.isBlank()) w.eq(Order::getSource, source);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(Order::getOrderNo, keyword).or().like(Order::getGuestName, keyword)
                    .or().like(Order::getGuestPhone, keyword));
        }
        if (startDate != null && !startDate.isBlank()) w.ge(Order::getCheckIn, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank()) w.le(Order::getCheckOut, LocalDate.parse(endDate));
        Page<Order> p = orderMapper.selectPage(Pages.of(page, size), w);
        return ApiResult.ok(new PageResult<>(enrich(p.getRecords()), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Order o = orderService.mustGet(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", enrich(List.of(o)).get(0));
        List<OrderGuest> guests = guestMapper.selectList(new LambdaQueryWrapper<OrderGuest>()
                .eq(OrderGuest::getOrderId, id));
        m.put("guests", guests.stream().map(g -> {
            Map<String, Object> gm = new LinkedHashMap<>();
            gm.put("id", g.getId());
            gm.put("name", g.getName());
            gm.put("phone", g.getPhone());
            gm.put("idType", g.getIdType());
            gm.put("idNoMask", crypto.mask(g.getIdNo()));
            gm.put("isMinor", g.getIsMinor());
            gm.put("guardianName", g.getGuardianName());
            return gm;
        }).toList());
        Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getOrderId, id).orderByDesc(Checkin::getId).last("limit 1"));
        if (c != null) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("roomId", c.getRoomId());
            cm.put("guestName", c.getGuestName());
            cm.put("idNoMask", crypto.mask(c.getIdNo()));
            cm.put("checkInTime", c.getCheckInTime());
            cm.put("expectCheckOut", c.getExpectCheckOut());
            cm.put("deposit", c.getDeposit());
            cm.put("cardNo", c.getCardNo());
            cm.put("status", c.getStatus());
            m.put("checkin", cm);
            m.put("deposits", depositMapper.selectList(new LambdaQueryWrapper<FinDeposit>()
                    .eq(FinDeposit::getCheckinId, c.getId()).orderByDesc(FinDeposit::getId)));
        }
        m.put("logs", statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, id).orderByAsc(OrderStatusLog::getId)));
        m.put("settlement", settlementMapper.selectOne(new LambdaQueryWrapper<FinSettlement>()
                .eq(FinSettlement::getOrderId, id).orderByDesc(FinSettlement::getId).last("limit 1")));
        return ApiResult.ok(m);
    }

    @PostMapping
    public ApiResult<Order> create(@RequestBody OrderDtos.CreateReq req) {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(orderService.create(tid, req, TenantContext.username()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<Order> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResult.ok(orderService.cancel(id, body == null ? null : body.get("reason")));
    }

    @PostMapping("/{id}/checkin")
    public ApiResult<Checkin> checkin(@PathVariable Long id, @RequestBody OrderDtos.CheckinReq req) {
        return ApiResult.ok(orderService.checkin(id, req));
    }

    @PostMapping("/{id}/checkout")
    public ApiResult<FinSettlement> checkout(@PathVariable Long id, @RequestBody(required = false) OrderDtos.CheckoutReq req) {
        return ApiResult.ok(orderService.checkout(id, req == null ? new OrderDtos.CheckoutReq() : req));
    }

    @PostMapping("/{id}/change-room")
    public ApiResult<Void> changeRoom(@PathVariable Long id, @RequestBody OrderDtos.ChangeRoomReq req) {
        orderService.changeRoom(id, req);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/extend")
    public ApiResult<Void> extend(@PathVariable Long id, @RequestBody OrderDtos.ExtendReq req) {
        orderService.extend(id, req);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/deposit")
    public ApiResult<FinDeposit> deposit(@PathVariable Long id, @RequestBody OrderDtos.DepositReq req) {
        return ApiResult.ok(orderService.deposit(id, req));
    }

    @GetMapping("/availability")
    public ApiResult<List<Map<String, Object>>> availability(@RequestParam Long roomTypeId,
                                                             @RequestParam String checkIn,
                                                             @RequestParam String checkOut) {
        Long tid = TenantScope.requiredTenantId();
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getTenantId, tid).eq(Room::getRoomTypeId, roomTypeId)
                .notIn(Room::getStatus, "REPAIR", "CLOSED"));
        BigDecimal price = orderService.calcAmount(tid, roomTypeId, in, out, 1);
        List<Map<String, Object>> available = rooms.stream()
                .filter(r -> orderService.roomAvailable(tid, r.getId(), in, out, null))
                .map(r -> Map.<String, Object>of("id", r.getId(), "roomNo", r.getRoomNo(),
                        "floor", r.getFloor(), "price", price, "status", r.getStatus()))
                .toList();
        return ApiResult.ok(available);
    }

    @GetMapping("/today")
    public ApiResult<Map<String, Object>> today() {
        Long tid = TenantScope.requiredTenantId();
        LocalDate today = LocalDate.now();
        List<Order> arrivals = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getCheckIn, today)
                .in(Order::getStatus, "CONFIRMED", "PENDING").orderByAsc(Order::getId));
        List<Order> departures = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getCheckOut, today)
                .eq(Order::getStatus, "CHECKED_IN").orderByAsc(Order::getId));
        List<Checkin> inHouse = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getTenantId, tid).eq(Checkin::getStatus, "IN_HOUSE"));
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
        return ApiResult.ok(Map.of(
                "arrivals", enrich(arrivals),
                "departures", enrich(departures),
                "inHouseCount", inHouse.size(),
                "vacantCount", rooms.stream().filter(r -> "VACANT".equals(r.getStatus())).count(),
                "dirtyCount", rooms.stream().filter(r -> "DIRTY".equals(r.getStatus())).count()
        ));
    }

    private List<Map<String, Object>> enrich(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        Long tid = TenantScope.requiredTenantId();
        Map<Long, String> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        Map<Long, String> types = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, String> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                        .eq(Room::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Room::getId, Room::getRoomNo));
        return orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("storeId", o.getStoreId());
            m.put("storeName", stores.getOrDefault(o.getStoreId(), ""));
            m.put("source", o.getSource());
            m.put("channel", o.getChannel());
            m.put("roomTypeId", o.getRoomTypeId());
            m.put("roomTypeName", types.getOrDefault(o.getRoomTypeId(), ""));
            m.put("roomId", o.getRoomId());
            m.put("roomNo", rooms.getOrDefault(o.getRoomId(), "待分配"));
            m.put("memberId", o.getMemberId());
            m.put("guestName", o.getGuestName());
            m.put("guestPhone", o.getGuestPhone());
            m.put("checkIn", o.getCheckIn());
            m.put("checkOut", o.getCheckOut());
            m.put("nights", o.getNights());
            m.put("roomCount", o.getRoomCount());
            m.put("amount", o.getAmount());
            m.put("paidAmount", o.getPaidAmount());
            m.put("status", o.getStatus());
            m.put("payType", o.getPayType());
            m.put("company", o.getCompany());
            m.put("remark", o.getRemark());
            m.put("createdBy", o.getCreatedBy());
            m.put("createTime", o.getCreateTime());
            return m;
        }).toList();
    }
}
