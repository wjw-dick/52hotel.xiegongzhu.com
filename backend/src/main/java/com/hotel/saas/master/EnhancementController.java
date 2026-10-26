package com.hotel.saas.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/enhance")
@RequiredArgsConstructor
public class EnhancementController {
    private final ChannelMapper channelMapper;
    private final DiningOrderMapper diningMapper;
    private final RoomMapper roomMapper;
    private final FinCashierMapper cashierMapper;

    @GetMapping("/channels")
    public ApiResult<List<Channel>> channels() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getTenantId, tid).orderByAsc(Channel::getId)));
    }

    @PostMapping("/channels/{id}/sync")
    public ApiResult<Channel> sync(@PathVariable Long id) {
        Channel c = channelMapper.selectById(id);
        if (c == null) throw new BizException(404, "渠道不存在");
        c.setLastSync(LocalDateTime.now());
        channelMapper.updateById(c);
        return ApiResult.ok(c);
    }

    @Data
    public static class ChannelReq {
        private String name;
        private String code;
        private Integer syncStock = 1;
    }

    @PostMapping("/channels")
    public ApiResult<Channel> addChannel(@RequestBody ChannelReq req) {
        Channel c = new Channel();
        c.setTenantId(TenantScope.requiredTenantId());
        c.setName(req.getName());
        c.setCode(req.getCode());
        c.setStatus(1);
        c.setSyncStock(req.getSyncStock());
        channelMapper.insert(c);
        return ApiResult.ok(c);
    }

    @GetMapping("/dining")
    public ApiResult<List<DiningOrder>> dining() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(diningMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                .eq(DiningOrder::getTenantId, tid).orderByDesc(DiningOrder::getId).last("limit 100")));
    }

    @Data
    public static class DiningReq {
        private Long storeId;
        private String roomNo;
        private String items;
        private BigDecimal amount;
    }

    @PostMapping("/dining")
    public ApiResult<DiningOrder> createDining(@RequestBody DiningReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        DiningOrder d = new DiningOrder();
        d.setTenantId(tid);
        d.setStoreId(req.getStoreId() == null ? 1L : req.getStoreId());
        d.setRoomNo(req.getRoomNo());
        d.setItems(req.getItems());
        d.setAmount(req.getAmount());
        d.setStatus("CREATED");
        d.setSettleType("ROOM");
        diningMapper.insert(d);
        return ApiResult.ok(d);
    }

    @PostMapping("/dining/{id}/charge-room")
    public ApiResult<DiningOrder> chargeRoom(@PathVariable Long id) {
        DiningOrder d = diningMapper.selectById(id);
        if (d == null) throw new BizException(404, "餐饮订单不存在");
        if ("CHARGED".equals(d.getStatus())) throw new BizException("该订单已挂账");
        d.setStatus("CHARGED");
        diningMapper.updateById(d);
        FinCashier c = new FinCashier();
        c.setTenantId(d.getTenantId());
        c.setStoreId(d.getStoreId());
        c.setBizType("DINING_AR");
        c.setAmount(d.getAmount());
        c.setPayType("ROOM");
        c.setOrderNo("DINING-" + d.getId());
        c.setOperator(TenantContext.username());
        cashierMapper.insert(c);
        return ApiResult.ok(d);
    }

    @PostMapping("/rooms/{id}/unlock")
    public ApiResult<Map<String, Object>> unlock(@PathVariable Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BizException(404, "房间不存在");
        return ApiResult.ok(Map.of("success", true, "roomNo", room.getRoomNo(),
                "message", "已下发远程开门指令（演示环境为模拟指令）"));
    }
}
