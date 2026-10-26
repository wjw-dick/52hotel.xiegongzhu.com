package com.hotel.saas.member;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.booking.OrderDtos;
import com.hotel.saas.booking.OrderService;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final TenantMapper tenantMapper;
    private final StoreMapper storeMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RateMapper rateMapper;
    private final TenantConfigMapper configMapper;
    private final MenuConfigMapper menuMapper;
    private final I18nMapper i18nMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final RateLimiter rateLimiter;
    private final JwtUtil jwtUtil;

    @GetMapping("/{tenantCode}/home")
    public ApiResult<Map<String, Object>> home(@PathVariable String tenantCode) {
        Tenant tenant = tenant(tenantCode);
        return TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "PUBLIC", () -> homeData(tenant));
    }

    private ApiResult<Map<String, Object>> homeData(Tenant tenant) {
        Long tid = tenant.getId();
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tid).eq(Store::getStatus, 1).orderByAsc(Store::getId));
        List<RoomType> roomTypes = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getTenantId, tid).eq(RoomType::getStatus, 1).orderByAsc(RoomType::getSort));
        Map<Long, String> storeNames = stores.stream().collect(Collectors.toMap(Store::getId, Store::getName));
        Map<String, String> config = new LinkedHashMap<>();
        configMapper.selectList(new LambdaQueryWrapper<TenantConfig>().eq(TenantConfig::getTenantId, tid))
                .forEach(c -> config.put(c.getConfigKey(), c.getConfigValue()));
        List<Map<String, Object>> roomRows = roomTypes.stream().map(rt -> roomRow(rt, storeNames.get(rt.getStoreId()))).toList();
        List<MenuConfig> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuConfig>()
                .eq(MenuConfig::getTenantId, tid).eq(MenuConfig::getPlatform, "H5")
                .eq(MenuConfig::getEnabled, 1).orderByAsc(MenuConfig::getSort));
        Map<String, String> i18n = new LinkedHashMap<>();
        i18nMapper.selectList(new LambdaQueryWrapper<I18n>().eq(I18n::getTenantId, tid))
                .forEach(i -> i18n.put(i.getLang() + ":" + i.getMsgKey(), i.getMsgValue()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenant", Map.of("code", tenant.getCode(), "name", tenant.getName(), "type", tenant.getType()));
        result.put("config", config);
        result.put("stores", stores);
        result.put("roomTypes", roomRows);
        result.put("menus", menus);
        result.put("i18n", i18n);
        result.put("announcement", config.getOrDefault("announcement",
                "秋日山居计划：连住两晚立减 100 元，会员额外享延迟退房。"));
        return ApiResult.ok(result);
    }

    @GetMapping("/{tenantCode}/stores/{storeId}")
    public ApiResult<Map<String, Object>> store(@PathVariable String tenantCode, @PathVariable Long storeId) {
        Tenant tenant = tenant(tenantCode);
        return TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "PUBLIC", () -> {
            Store store = storeMapper.selectById(storeId);
            if (store == null || !store.getTenantId().equals(tenant.getId())) throw new BizException(404, "门店不存在");
            List<RoomType> types = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                    .eq(RoomType::getTenantId, tenant.getId()).eq(RoomType::getStoreId, storeId)
                    .eq(RoomType::getStatus, 1).orderByAsc(RoomType::getSort));
            return ApiResult.ok(Map.of("store", store,
                    "roomTypes", types.stream().map(rt -> roomRow(rt, store.getName())).toList()));
        });
    }

    @GetMapping("/{tenantCode}/room-types/{id}")
    public ApiResult<Map<String, Object>> roomType(@PathVariable String tenantCode, @PathVariable Long id) {
        Tenant tenant = tenant(tenantCode);
        return TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "PUBLIC", () -> {
            RoomType rt = roomTypeMapper.selectById(id);
            if (rt == null || !rt.getTenantId().equals(tenant.getId())) throw new BizException(404, "房型不存在");
            Store store = storeMapper.selectById(rt.getStoreId());
            List<Map<String, Object>> calendar = new ArrayList<>();
            for (int d = 0; d < 30; d++) {
                LocalDate date = LocalDate.now().plusDays(d);
                Rate rate = rateMapper.selectOne(new LambdaQueryWrapper<Rate>()
                        .eq(Rate::getTenantId, tenant.getId()).eq(Rate::getRoomTypeId, id)
                        .eq(Rate::getRateDate, date).eq(Rate::getPriceType, "BAR").last("limit 1"));
                calendar.add(Map.of("date", date.toString(),
                        "price", rate == null ? rt.getBasePrice() : rate.getPrice(),
                        "available", roomServiceAvailable(tenant.getId(), id, date)));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("roomType", roomRow(rt, store == null ? "" : store.getName()));
            result.put("store", store);
            result.put("calendar", calendar);
            return ApiResult.ok(result);
        });
    }

    @GetMapping("/{tenantCode}/availability")
    public ApiResult<Map<String, Object>> availability(@PathVariable String tenantCode,
                                                       @RequestParam Long roomTypeId,
                                                       @RequestParam String checkIn,
                                                       @RequestParam String checkOut) {
        Tenant tenant = tenant(tenantCode);
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        if (!out.isAfter(in)) throw new BizException("离店日期必须晚于入住日期");
        List<Map<String, Object>> rooms = TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "CUSTOMER", () -> {
            List<Room> list = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                    .eq(Room::getTenantId, tenant.getId()).eq(Room::getRoomTypeId, roomTypeId)
                    .notIn(Room::getStatus, "REPAIR", "CLOSED", "OCCUPIED").orderByAsc(Room::getRoomNo));
            return list.stream().filter(r -> orderService.roomAvailable(tenant.getId(), r.getId(), in, out, null))
                    .map(r -> Map.<String, Object>of("id", r.getId(), "roomNo", r.getRoomNo(),
                            "floor", r.getFloor(), "status", r.getStatus()))
                    .toList();
        });
        BigDecimal price = TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "CUSTOMER",
                () -> orderService.calcAmount(tenant.getId(), roomTypeId, in, out, 1));
        return ApiResult.ok(Map.of("rooms", rooms, "price", price, "nights", out.toEpochDay() - in.toEpochDay()));
    }

    @PostMapping("/{tenantCode}/orders")
    public ApiResult<Order> createOrder(@PathVariable String tenantCode, @RequestBody OrderDtos.CreateReq req,
                                        HttpServletRequest request) {
        Tenant tenant = tenant(tenantCode);
        rateLimiter.check("public-order:" + clientIp(request), 15);
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            rateLimiter.check("dup:" + requestId, 1);
        }
        if (req.getIdNo() == null || req.getIdNo().length() < 15) throw new BizException("请填写有效身份证号");
        Order order = TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "CUSTOMER",
                () -> orderService.create(tenant.getId(), req, "H5/小程序"));
        return ApiResult.ok(order);
    }

    @GetMapping("/{tenantCode}/orders/query")
    public ApiResult<Map<String, Object>> queryOrder(@PathVariable String tenantCode,
                                                     @RequestParam String orderNo,
                                                     @RequestParam String phone) {
        Tenant tenant = tenant(tenantCode);
        return TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "PUBLIC", () -> {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getTenantId, tenant.getId()).eq(Order::getOrderNo, orderNo).last("limit 1"));
            if (order == null) throw new BizException(404, "订单不存在");
            String storedPhone = order.getGuestPhone() == null ? "" : order.getGuestPhone();
            if (!storedPhone.equals(phone) && !storedPhone.endsWith(phone)) {
                throw new BizException(403, "手机号与订单不匹配");
            }
            return ApiResult.ok(Map.of("order", order));
        });
    }

    @GetMapping("/{tenantCode}/i18n")
    public ApiResult<Map<String, String>> i18n(@PathVariable String tenantCode, @RequestParam(defaultValue = "zh-CN") String lang) {
        Tenant tenant = tenant(tenantCode);
        return TenantContext.runAsTenant(tenant.getId(), tenant.getCode(), "PUBLIC", () -> {
            Map<String, String> map = new LinkedHashMap<>();
            i18nMapper.selectList(new LambdaQueryWrapper<I18n>()
                            .eq(I18n::getTenantId, tenant.getId()).eq(I18n::getLang, lang))
                    .forEach(i -> map.put(i.getMsgKey(), i.getMsgValue()));
            return ApiResult.ok(map);
        });
    }

    private Tenant tenant(String code) {
        Tenant t = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code).last("limit 1"));
        if (t == null) throw new BizException(404, "租户不存在");
        if (!"NORMAL".equals(t.getStatus()) && !"TRIAL".equals(t.getStatus())) throw new BizException(403, "门店暂停营业");
        return t;
    }

    private Map<String, Object> roomRow(RoomType rt, String storeName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rt.getId());
        m.put("storeId", rt.getStoreId());
        m.put("storeName", storeName);
        m.put("name", rt.getName());
        m.put("code", rt.getCode());
        m.put("area", rt.getArea());
        m.put("capacity", rt.getCapacity());
        m.put("bedType", rt.getBedType());
        m.put("facilities", rt.getFacilities() == null ? List.of() : List.of(rt.getFacilities().split(",")));
        m.put("cover", rt.getCover());
        m.put("images", rt.getImages());
        m.put("basePrice", rt.getBasePrice());
        m.put("intro", rt.getIntro());
        return m;
    }

    private boolean roomServiceAvailable(Long tid, Long typeId, LocalDate date) {
        Long rooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getTenantId, tid).eq(Room::getRoomTypeId, typeId)
                .notIn(Room::getStatus, "REPAIR", "CLOSED"));
        return rooms != null && rooms > 0;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }
}
