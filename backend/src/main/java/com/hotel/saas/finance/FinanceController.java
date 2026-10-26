package com.hotel.saas.finance;

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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final FinSettlementMapper settlementMapper;
    private final FinCashierMapper cashierMapper;
    private final FinArMapper arMapper;
    private final FinArLedgerMapper arLedgerMapper;
    private final FinShiftMapper shiftMapper;
    private final FinDepositMapper depositMapper;
    private final OrderMapper orderMapper;
    private final StoreMapper storeMapper;
    private final CheckinMapper checkinMapper;

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        Long tid = TenantScope.requiredTenantId();
        List<FinSettlement> settlements = settlementMapper.selectList(new LambdaQueryWrapper<FinSettlement>()
                .eq(FinSettlement::getTenantId, tid));
        BigDecimal total = settlements.stream().map(FinSettlement::getTotal)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal today = settlements.stream()
                .filter(s -> s.getCreateTime() != null && s.getCreateTime().toLocalDate().equals(java.time.LocalDate.now()))
                .map(FinSettlement::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal arTotal = arMapper.selectList(new LambdaQueryWrapper<FinAr>().eq(FinAr::getTenantId, tid))
                .stream().map(FinAr::getBalance).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal depositTotal = depositMapper.selectList(new LambdaQueryWrapper<FinDeposit>()
                        .eq(FinDeposit::getTenantId, tid)).stream()
                .map(d -> "REFUND".equals(d.getType()) ? d.getAmount().negate() : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ApiResult.ok(Map.of("totalRevenue", total, "todayRevenue", today,
                "arBalance", arTotal, "depositBalance", depositTotal,
                "settlementCount", settlements.size()));
    }

    @GetMapping("/settlements")
    public ApiResult<List<Map<String, Object>>> settlements(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<FinSettlement> w = new LambdaQueryWrapper<FinSettlement>()
                .eq(FinSettlement::getTenantId, tid).orderByDesc(FinSettlement::getId).last("limit 300");
        if (startDate != null && !startDate.isBlank()) w.ge(FinSettlement::getCreateTime, startDate + " 00:00:00");
        if (endDate != null && !endDate.isBlank()) w.le(FinSettlement::getCreateTime, endDate + " 23:59:59");
        List<FinSettlement> list = settlementMapper.selectList(w);
        Map<Long, Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getTenantId, tid))
                .stream().collect(Collectors.toMap(Order::getId, o -> o));
        return ApiResult.ok(list.stream().map(s -> {
            Order o = orders.get(s.getOrderId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("orderId", s.getOrderId());
            m.put("orderNo", o == null ? "" : o.getOrderNo());
            m.put("guestName", o == null ? "" : o.getGuestName());
            m.put("roomFee", s.getRoomFee());
            m.put("depositDeduct", s.getDepositDeduct());
            m.put("discount", s.getDiscount());
            m.put("otherFee", s.getOtherFee());
            m.put("total", s.getTotal());
            m.put("payType", s.getPayType());
            m.put("operator", s.getOperator());
            m.put("createTime", s.getCreateTime());
            return m;
        }).toList());
    }

    @GetMapping("/cashier")
    public ApiResult<List<FinCashier>> cashier() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(cashierMapper.selectList(new LambdaQueryWrapper<FinCashier>()
                .eq(FinCashier::getTenantId, tid).orderByDesc(FinCashier::getId).last("limit 200")));
    }

    @GetMapping("/deposits")
    public ApiResult<List<Map<String, Object>>> deposits() {
        Long tid = TenantScope.requiredTenantId();
        List<FinDeposit> list = depositMapper.selectList(new LambdaQueryWrapper<FinDeposit>()
                .eq(FinDeposit::getTenantId, tid).orderByDesc(FinDeposit::getId).last("limit 200"));
        Map<Long, Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Checkin::getId, c -> c));
        return ApiResult.ok(list.stream().map(d -> {
            Checkin c = checkins.get(d.getCheckinId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("guestName", c == null ? "" : c.getGuestName());
            m.put("amount", d.getAmount());
            m.put("type", d.getType());
            m.put("payType", d.getPayType());
            m.put("operator", d.getOperator());
            m.put("createTime", d.getCreateTime());
            return m;
        }).toList());
    }

    @GetMapping("/ar")
    public ApiResult<List<FinAr>> arList() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(arMapper.selectList(new LambdaQueryWrapper<FinAr>()
                .eq(FinAr::getTenantId, tid).orderByDesc(FinAr::getId)));
    }

    @Data
    public static class ArReq {
        private Long id;
        private String company;
        private String contact;
        private String phone;
        private String remark;
    }

    @PostMapping("/ar")
    public ApiResult<FinAr> createAr(@RequestBody ArReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getCompany() == null || req.getCompany().isBlank()) throw new BizException("请填写挂账单位");
        FinAr ar = new FinAr();
        ar.setTenantId(tid);
        ar.setCompany(req.getCompany());
        ar.setContact(req.getContact());
        ar.setPhone(req.getPhone());
        ar.setBalance(BigDecimal.ZERO);
        ar.setStatus("NORMAL");
        arMapper.insert(ar);
        return ApiResult.ok(ar);
    }

    @GetMapping("/ar/{id}/ledger")
    public ApiResult<List<FinArLedger>> ledger(@PathVariable Long id) {
        return ApiResult.ok(arLedgerMapper.selectList(new LambdaQueryWrapper<FinArLedger>()
                .eq(FinArLedger::getArId, id).orderByDesc(FinArLedger::getId)));
    }

    @Data
    public static class ArTradeReq {
        private BigDecimal amount;
        private String type = "CHARGE";
        private String remark;
    }

    @PostMapping("/ar/{id}/trade")
    public ApiResult<FinAr> arTrade(@PathVariable Long id, @RequestBody ArTradeReq req) {
        FinAr ar = arMapper.selectById(id);
        if (ar == null) throw new BizException(404, "挂账客户不存在");
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        BigDecimal balance;
        if ("VERIFY".equals(req.getType())) {
            if (req.getAmount().compareTo(ar.getBalance()) > 0) throw new BizException("核销金额不能超过挂账余额");
            balance = ar.getBalance().subtract(req.getAmount());
        } else {
            balance = ar.getBalance().add(req.getAmount());
        }
        ar.setBalance(balance);
        arMapper.updateById(ar);
        FinArLedger ledger = new FinArLedger();
        ledger.setTenantId(ar.getTenantId());
        ledger.setArId(id);
        ledger.setType(req.getType());
        ledger.setAmount(req.getAmount());
        ledger.setRemark(req.getRemark());
        ledger.setOperator(TenantContext.username());
        arLedgerMapper.insert(ledger);
        return ApiResult.ok(ar);
    }

    @GetMapping("/shifts")
    public ApiResult<List<FinShift>> shifts() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(shiftMapper.selectList(new LambdaQueryWrapper<FinShift>()
                .eq(FinShift::getTenantId, tid).orderByDesc(FinShift::getId).last("limit 100")));
    }

    @Data
    public static class ShiftReq {
        private Long storeId;
        private String shiftName = "早班";
        private BigDecimal cashAmount;
    }

    @PostMapping("/shifts/open")
    public ApiResult<FinShift> openShift(@RequestBody ShiftReq req) {
        Long tid = TenantScope.requiredTenantId();
        Long open = shiftMapper.selectCount(new LambdaQueryWrapper<FinShift>()
                .eq(FinShift::getTenantId, tid).eq(FinShift::getStatus, "OPEN"));
        if (open != null && open > 0) throw new BizException("已有未关闭的班次，请先交班");
        FinShift s = new FinShift();
        s.setTenantId(tid);
        s.setStoreId(req.getStoreId() == null ? 1L : req.getStoreId());
        s.setShiftName(req.getShiftName());
        s.setOperator(TenantContext.username());
        s.setStartTime(LocalDateTime.now());
        s.setCashAmount(req.getCashAmount() == null ? BigDecimal.ZERO : req.getCashAmount());
        s.setStatus("OPEN");
        shiftMapper.insert(s);
        return ApiResult.ok(s);
    }

    @PostMapping("/shifts/{id}/close")
    public ApiResult<FinShift> closeShift(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        FinShift s = shiftMapper.selectById(id);
        if (s == null) throw new BizException(404, "班次不存在");
        if (!"OPEN".equals(s.getStatus())) throw new BizException("班次已关闭");
        s.setStatus("CLOSED");
        s.setEndTime(LocalDateTime.now());
        if (body != null && body.get("cashAmount") != null) {
            s.setCashAmount(new BigDecimal(body.get("cashAmount").toString()));
        }
        shiftMapper.updateById(s);
        return ApiResult.ok(s);
    }

    @GetMapping("/stores")
    public ApiResult<List<Store>> stores() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getTenantId, tid)));
    }
}
