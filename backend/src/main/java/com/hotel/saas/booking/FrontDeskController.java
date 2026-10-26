package com.hotel.saas.booking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class FrontDeskController {
    private final CheckinMapper checkinMapper;
    private final PoliceReportMapper policeReportMapper;
    private final StayChangeMapper stayChangeMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final StoreMapper storeMapper;
    private final CryptoService crypto;

    @GetMapping("/checkins")
    public ApiResult<List<Map<String, Object>>> checkins(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<Checkin> w = new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getTenantId, tid).orderByDesc(Checkin::getId);
        if (status != null && !status.isBlank()) w.eq(Checkin::getStatus, status);
        if (keyword != null && !keyword.isBlank()) w.like(Checkin::getGuestName, keyword);
        List<Checkin> list = checkinMapper.selectList(w);
        Map<Long, String> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid))
                .stream().collect(Collectors.toMap(Room::getId, Room::getRoomNo));
        Map<Long, Room> roomMap = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid))
                .stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, String> types = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, String> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        return ApiResult.ok(list.stream().map(c -> {
            Room room = roomMap.get(c.getRoomId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("orderId", c.getOrderId());
            m.put("storeId", c.getStoreId());
            m.put("storeName", stores.getOrDefault(c.getStoreId(), ""));
            m.put("roomId", c.getRoomId());
            m.put("roomNo", rooms.getOrDefault(c.getRoomId(), ""));
            m.put("roomTypeName", room == null ? "" : types.getOrDefault(room.getRoomTypeId(), ""));
            m.put("guestName", c.getGuestName());
            m.put("idNoMask", crypto.mask(c.getIdNo()));
            m.put("checkInTime", c.getCheckInTime());
            m.put("expectCheckOut", c.getExpectCheckOut());
            m.put("deposit", c.getDeposit());
            m.put("cardNo", c.getCardNo());
            m.put("minorInfo", c.getMinorInfo());
            m.put("status", c.getStatus());
            return m;
        }).toList());
    }

    @GetMapping("/checkins/{id}/changes")
    public ApiResult<List<StayChange>> changes(@PathVariable Long id) {
        return ApiResult.ok(stayChangeMapper.selectList(new LambdaQueryWrapper<StayChange>()
                .eq(StayChange::getCheckinId, id).orderByDesc(StayChange::getId)));
    }

    @GetMapping("/police-reports")
    public ApiResult<List<Map<String, Object>>> reports(@RequestParam(required = false) String keyword) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<PoliceReport> w = new LambdaQueryWrapper<PoliceReport>()
                .eq(PoliceReport::getTenantId, tid).orderByDesc(PoliceReport::getId).last("limit 200");
        if (keyword != null && !keyword.isBlank()) w.like(PoliceReport::getGuestName, keyword);
        return ApiResult.ok(policeReportMapper.selectList(w).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("guestName", p.getGuestName());
            m.put("idNoMask", p.getIdNo());
            m.put("roomNo", p.getRoomNo());
            m.put("reportStatus", p.getReportStatus());
            m.put("reportTime", p.getReportTime());
            m.put("response", p.getResponse());
            return m;
        }).toList());
    }

    @PostMapping("/police-reports/{id}/retry")
    public ApiResult<Void> retry(@PathVariable Long id) {
        PoliceReport report = policeReportMapper.selectById(id);
        if (report == null) throw new BizException(404, "报送记录不存在");
        report.setReportStatus("SUCCESS");
        report.setReportTime(LocalDateTime.now());
        report.setResponse("模拟重新报送成功（演示环境，真实环境对接公安旅馆业系统）");
        policeReportMapper.updateById(report);
        return ApiResult.ok();
    }
}
