package com.hotel.saas.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.Rate;
import com.hotel.saas.domain.RateLog;
import com.hotel.saas.domain.RoomType;
import com.hotel.saas.mapper.RateLogMapper;
import com.hotel.saas.mapper.RateMapper;
import com.hotel.saas.mapper.RoomTypeMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rates")
@RequiredArgsConstructor
public class RateController {
    private final RateMapper rateMapper;
    private final RateLogMapper rateLogMapper;
    private final RoomTypeMapper roomTypeMapper;

    @Data
    public static class BatchReq {
        private Long roomTypeId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String priceType = "BAR";
        private BigDecimal price;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> calendar(@RequestParam Long roomTypeId,
                                                   @RequestParam String month) {
        Long tid = TenantScope.requiredTenantId();
        RoomType rt = roomTypeMapper.selectById(roomTypeId);
        if (rt == null) throw new BizException(404, "房型不存在");
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<Rate> rates = rateMapper.selectList(new LambdaQueryWrapper<Rate>()
                .eq(Rate::getTenantId, tid).eq(Rate::getRoomTypeId, roomTypeId)
                .between(Rate::getRateDate, start, end).orderByAsc(Rate::getRateDate));
        Map<String, Map<String, BigDecimal>> calendar = new LinkedHashMap<>();
        for (Rate r : rates) {
            calendar.computeIfAbsent(r.getRateDate().toString(), k -> new LinkedHashMap<>())
                    .put(r.getPriceType(), r.getPrice());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomType", rt);
        result.put("month", month);
        result.put("calendar", calendar);
        return ApiResult.ok(result);
    }

    @PostMapping("/batch")
    @Transactional
    public ApiResult<Map<String, Object>> batch(@RequestBody BatchReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getRoomTypeId() == null || req.getStartDate() == null || req.getPrice() == null) {
            throw new BizException("房型、开始日期、价格不能为空");
        }
        LocalDate end = req.getEndDate() == null ? req.getStartDate() : req.getEndDate();
        if (end.isBefore(req.getStartDate()) || end.isAfter(req.getStartDate().plusMonths(6))) {
            throw new BizException("日期区间不合法（最长支持 6 个月）");
        }
        int changed = 0;
        for (LocalDate d = req.getStartDate(); !d.isAfter(end); d = d.plusDays(1)) {
            Rate rate = rateMapper.selectOne(new LambdaQueryWrapper<Rate>()
                    .eq(Rate::getTenantId, tid).eq(Rate::getRoomTypeId, req.getRoomTypeId())
                    .eq(Rate::getRateDate, d).eq(Rate::getPriceType, req.getPriceType()).last("limit 1"));
            BigDecimal old = rate == null ? null : rate.getPrice();
            if (rate == null) {
                rate = new Rate();
                rate.setTenantId(tid);
                rate.setRoomTypeId(req.getRoomTypeId());
                rate.setRateDate(d);
                rate.setPriceType(req.getPriceType());
                rate.setPrice(req.getPrice());
                rateMapper.insert(rate);
            } else {
                rate.setPrice(req.getPrice());
                rateMapper.updateById(rate);
            }
            RateLog log = new RateLog();
            log.setTenantId(tid);
            log.setRoomTypeId(req.getRoomTypeId());
            log.setRateDate(d);
            log.setPriceType(req.getPriceType());
            log.setOldPrice(old);
            log.setNewPrice(req.getPrice());
            log.setOperator(TenantContext.username());
            rateLogMapper.insert(log);
            changed++;
        }
        return ApiResult.ok(Map.of("changed", changed));
    }

    @GetMapping("/logs")
    public ApiResult<List<Map<String, Object>>> logs(@RequestParam(required = false) Long roomTypeId) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<RateLog> w = new LambdaQueryWrapper<RateLog>()
                .eq(RateLog::getTenantId, tid).orderByDesc(RateLog::getCreateTime).last("limit 200");
        if (roomTypeId != null) w.eq(RateLog::getRoomTypeId, roomTypeId);
        List<RateLog> logs = rateLogMapper.selectList(w);
        Map<Long, String> names = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        return ApiResult.ok(logs.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("roomTypeName", names.getOrDefault(l.getRoomTypeId(), ""));
            m.put("rateDate", l.getRateDate());
            m.put("priceType", l.getPriceType());
            m.put("oldPrice", l.getOldPrice());
            m.put("newPrice", l.getNewPrice());
            m.put("operator", l.getOperator());
            m.put("createTime", l.getCreateTime());
            return m;
        }).toList());
    }
}
