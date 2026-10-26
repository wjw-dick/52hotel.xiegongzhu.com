package com.hotel.saas.booking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderGuestMapper guestMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final RoomStatusLogMapper statusLogMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RateMapper rateMapper;
    private final StoreMapper storeMapper;
    private final CheckinMapper checkinMapper;
    private final PoliceReportMapper policeReportMapper;
    private final StayChangeMapper stayChangeMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinCashierMapper cashierMapper;
    private final FinDepositMapper depositMapper;
    private final HkTaskMapper taskMapper;
    private final MemMemberMapper memberMapper;
    private final MemPointLogMapper pointLogMapper;
    private final NotificationMapper notificationMapper;
    private final NoGenerator noGenerator;
    private final CryptoService crypto;
    private final StringRedisTemplate redis;

    @Value("${app.demo.seed:true}")
    private boolean demo;

    @Transactional
    public Order create(Long tid, OrderDtos.CreateReq req, String operator) {
        validateCreate(req);
        RoomType type = roomTypeMapper.selectById(req.getRoomTypeId());
        if (type == null) throw new BizException(404, "房型不存在");
        if (req.getStoreId() == null) req.setStoreId(type.getStoreId());
        if (type.getStoreId() != null && !type.getStoreId().equals(req.getStoreId())) {
            throw new BizException("房型与门店不匹配");
        }
        Room room = chooseRoom(tid, req);
        String orderNo = lockAndNextNo(tid, req.getStoreId());
        int nights = (int) (req.getCheckOut().toEpochDay() - req.getCheckIn().toEpochDay());
        BigDecimal amount = calcAmount(tid, req.getRoomTypeId(), req.getCheckIn(), req.getCheckOut(), req.getRoomCount());
        if (req.getDiscount() != null && req.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            amount = amount.subtract(req.getDiscount()).max(BigDecimal.ZERO);
        }
        Order o = new Order();
        o.setTenantId(tid);
        o.setStoreId(req.getStoreId());
        o.setOrderNo(orderNo);
        o.setSource(req.getSource() == null ? "WALK_IN" : req.getSource());
        o.setChannel(req.getChannel());
        o.setRoomTypeId(req.getRoomTypeId());
        o.setRoomId(room == null ? null : room.getId());
        o.setMemberId(req.getMemberId());
        o.setGuestName(req.getGuestName());
        o.setGuestPhone(req.getGuestPhone());
        o.setCheckIn(req.getCheckIn());
        o.setCheckOut(req.getCheckOut());
        o.setNights(nights);
        o.setRoomCount(req.getRoomCount() == null ? 1 : req.getRoomCount());
        o.setAmount(amount);
        o.setPaidAmount("ONLINE".equals(req.getPayType()) ? amount : BigDecimal.ZERO);
        o.setStatus("WAITLIST".equals(req.getSource()) ? "WAITLIST" : "CONFIRMED");
        o.setPayType(req.getPayType() == null ? "ARRIVAL" : req.getPayType());
        o.setCompany(req.getCompany());
        o.setRemark(req.getRemark());
        o.setCreatedBy(operator);
        orderMapper.insert(o);

        OrderGuest guest = new OrderGuest();
        guest.setTenantId(tid);
        guest.setOrderId(o.getId());
        guest.setName(req.getGuestName());
        guest.setPhone(req.getGuestPhone());
        guest.setIdNo(crypto.encrypt(req.getIdNo()));
        guest.setIdType("ID_CARD");
        guest.setIsMinor(0);
        guestMapper.insert(guest);

        if (room != null && !"WAITLIST".equals(o.getStatus())) {
            updateRoomStatus(tid, room, "RESERVED", "预订占用");
        }
        statusLog(tid, o.getId(), null, o.getStatus(), operator, "创建订单");
        notify(tid, "预订确认", "订单 " + orderNo + " 已创建，" + req.getCheckIn() + " 入住 " + type.getName());
        return o;
    }

    public BigDecimal calcAmount(Long tid, Long roomTypeId, LocalDate in, LocalDate out, Integer roomCount) {
        BigDecimal total = BigDecimal.ZERO;
        int count = roomCount == null ? 1 : roomCount;
        for (LocalDate d = in; d.isBefore(out); d = d.plusDays(1)) {
            Rate rate = rateMapper.selectOne(new LambdaQueryWrapper<Rate>()
                    .eq(Rate::getTenantId, tid).eq(Rate::getRoomTypeId, roomTypeId)
                    .eq(Rate::getRateDate, d).eq(Rate::getPriceType, "BAR").last("limit 1"));
            if (rate != null) {
                total = total.add(rate.getPrice());
            } else {
                RoomType type = roomTypeMapper.selectById(roomTypeId);
                total = total.add(type == null ? new BigDecimal("500") : type.getBasePrice());
            }
        }
        return total.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean roomAvailable(Long tid, Long roomId, LocalDate in, LocalDate out, Long excludeOrderId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) return false;
        if (Set.of("REPAIR", "CLOSED", "OCCUPIED").contains(room.getStatus())) return false;
        Long overlap = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tid).eq(Order::getRoomId, roomId)
                .in(Order::getStatus, "CONFIRMED", "CHECKED_IN")
                .lt(Order::getCheckIn, out).gt(Order::getCheckOut, in)
                .ne(excludeOrderId != null, Order::getId, excludeOrderId));
        return overlap == null || overlap == 0;
    }

    private Room chooseRoom(Long tid, OrderDtos.CreateReq req) {
        if (req.getRoomId() != null) {
            Room room = roomMapper.selectById(req.getRoomId());
            if (room == null || !room.getTenantId().equals(tid)) throw new BizException("房间不存在");
            if (!room.getRoomTypeId().equals(req.getRoomTypeId())) throw new BizException("房间与房型不匹配");
            if (!roomAvailable(tid, room.getId(), req.getCheckIn(), req.getCheckOut(), null)) {
                throw new BizException("该房间在所选日期已被占用");
            }
            return room;
        }
        List<Room> candidates = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getTenantId, tid).eq(Room::getStoreId, req.getStoreId())
                .eq(Room::getRoomTypeId, req.getRoomTypeId())
                .notIn(Room::getStatus, "REPAIR", "CLOSED", "OCCUPIED")
                .orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo));
        return candidates.stream()
                .filter(r -> roomAvailable(tid, r.getId(), req.getCheckIn(), req.getCheckOut(), null))
                .findFirst().orElse(null);
    }

    private String lockAndNextNo(Long tid, Long storeId) {
        String lockKey = "hotel:lock:order:" + tid;
        String token = UUID.randomUUID().toString();
        boolean locked = false;
        try {
            locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(8)));
        } catch (Exception ignored) {
        }
        try {
            String tenantCode = TenantContext.get() != null && TenantContext.get().tenantCode() != null
                    ? TenantContext.get().tenantCode() : "HTL";
            String prefix = tenantCode + "-" + LocalDate.now().getYear() + "-";
            List<Order> existing = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .likeRight(Order::getOrderNo, prefix).select(Order::getOrderNo));
            int max = 0;
            for (Order o : existing) {
                try {
                    max = Math.max(max, Integer.parseInt(o.getOrderNo().substring(prefix.length())));
                } catch (Exception ignored) {
                }
            }
            return prefix + String.format("%04d", max + 1);
        } finally {
            if (locked) {
                try {
                    if (token.equals(redis.opsForValue().get(lockKey))) redis.delete(lockKey);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Transactional
    public Order cancel(Long id, String reason) {
        Order o = mustGet(id);
        if (!Set.of("PENDING", "CONFIRMED", "WAITLIST").contains(o.getStatus())) {
            throw new BizException("当前订单状态不可取消");
        }
        String from = o.getStatus();
        o.setStatus("CANCELLED");
        o.setRemark((o.getRemark() == null ? "" : o.getRemark() + "；") + "取消原因：" + (reason == null ? "用户取消" : reason));
        orderMapper.updateById(o);
        releaseRoom(o);
        statusLog(o.getTenantId(), id, from, "CANCELLED", TenantContext.username(), reason);
        notify(o.getTenantId(), "订单已取消", "订单 " + o.getOrderNo() + " 已取消，房态已释放");
        return o;
    }

    @Transactional
    public Checkin checkin(Long orderId, OrderDtos.CheckinReq req) {
        Order o = mustGet(orderId);
        if (!Set.of("CONFIRMED", "PENDING").contains(o.getStatus())) {
            throw new BizException("当前订单不可办理入住");
        }
        if (req.getIdNo() == null || req.getIdNo().length() < 15) {
            throw new BizException("请录入有效身份证件号码（实名登记为强制要求）");
        }
        Long roomId = req.getRoomId() == null ? o.getRoomId() : req.getRoomId();
        if (roomId == null) {
            Room picked = chooseRoom(o.getTenantId(), roomFor(o));
            if (picked == null) throw new BizException("暂无可用房间，请先在房态中释放房间");
            roomId = picked.getId();
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException("房间不存在");
        if (!room.getTenantId().equals(o.getTenantId())) throw new BizException(403, "跨租户操作被拦截");

        Checkin c = new Checkin();
        c.setTenantId(o.getTenantId());
        c.setOrderId(o.getId());
        c.setStoreId(o.getStoreId());
        c.setRoomId(roomId);
        c.setGuestName(req.getGuestName() == null ? o.getGuestName() : req.getGuestName());
        c.setIdNo(crypto.encrypt(req.getIdNo()));
        c.setCheckInTime(LocalDateTime.now());
        c.setExpectCheckOut(o.getCheckOut().atTime(12, 0));
        c.setDeposit(req.getDeposit() == null ? BigDecimal.ZERO : req.getDeposit());
        c.setDepositPayType(req.getDepositPayType());
        c.setCardNo(req.getCardNo() == null ? "CARD-" + roomId + "-" + System.currentTimeMillis() % 10000 : req.getCardNo());
        c.setMinorInfo(req.getMinorInfo());
        c.setStatus("IN_HOUSE");
        checkinMapper.insert(c);

        PoliceReport report = new PoliceReport();
        report.setTenantId(o.getTenantId());
        report.setCheckinId(c.getId());
        report.setGuestName(c.getGuestName());
        report.setIdNo(crypto.mask(c.getIdNo()));
        report.setRoomNo(room.getRoomNo());
        boolean policeEnabled = !"false".equalsIgnoreCase(paramValue("police_report_enabled", "true"));
        report.setReportStatus(policeEnabled ? "SUCCESS" : "PENDING");
        report.setReportTime(policeEnabled ? LocalDateTime.now() : null);
        report.setResponse(policeEnabled ? "模拟报送成功：已按旅馆业治安管理要求登记（演示环境）" : "平台已关闭实名报送");
        policeReportMapper.insert(report);

        if (c.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
            FinDeposit d = new FinDeposit();
            d.setTenantId(o.getTenantId());
            d.setCheckinId(c.getId());
            d.setAmount(c.getDeposit());
            d.setType("COLLECT");
            d.setPayType(c.getDepositPayType());
            d.setOperator(TenantContext.username());
            depositMapper.insert(d);
        }

        String from = o.getStatus();
        o.setStatus("CHECKED_IN");
        o.setRoomId(roomId);
        orderMapper.updateById(o);
        updateRoomStatus(o.getTenantId(), room, "OCCUPIED", "办理入住");
        statusLog(o.getTenantId(), o.getId(), from, "CHECKED_IN", TenantContext.username(), "入住登记");
        notify(o.getTenantId(), "入住提醒", c.getGuestName() + " 已入住 " + room.getRoomNo() + "，祝入住愉快");
        return c;
    }

    @Transactional
    public FinSettlement checkout(Long orderId, OrderDtos.CheckoutReq req) {
        Order o = mustGet(orderId);
        if (!"CHECKED_IN".equals(o.getStatus())) throw new BizException("订单未在住，无法退房");
        Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getOrderId, orderId).eq(Checkin::getStatus, "IN_HOUSE").last("limit 1"));
        if (c == null) throw new BizException("未找到在住登记");

        BigDecimal roomFee = calcAmount(o.getTenantId(), o.getRoomTypeId(), o.getCheckIn(), o.getCheckOut(), o.getRoomCount());
        BigDecimal depositDeduct = req.getDepositDeduct() == null ? BigDecimal.ZERO : req.getDepositDeduct();
        if (depositDeduct.compareTo(c.getDeposit()) > 0) throw new BizException("押金抵扣不能超过实收押金");
        BigDecimal discount = req.getDiscount() == null ? BigDecimal.ZERO : req.getDiscount();
        BigDecimal otherFee = req.getOtherFee() == null ? BigDecimal.ZERO : req.getOtherFee();
        BigDecimal total = roomFee.subtract(depositDeduct).subtract(discount).add(otherFee).max(BigDecimal.ZERO);

        FinSettlement s = new FinSettlement();
        s.setTenantId(o.getTenantId());
        s.setOrderId(o.getId());
        s.setCheckinId(c.getId());
        s.setRoomFee(roomFee);
        s.setDepositDeduct(depositDeduct);
        s.setDiscount(discount);
        s.setOtherFee(otherFee);
        s.setTotal(total);
        s.setPayType(req.getPayType());
        s.setStatus("PAID");
        s.setOperator(TenantContext.username());
        settlementMapper.insert(s);

        FinCashier cashier = new FinCashier();
        cashier.setTenantId(o.getTenantId());
        cashier.setStoreId(o.getStoreId());
        cashier.setBizType("ROOM");
        cashier.setAmount(total);
        cashier.setPayType(req.getPayType());
        cashier.setOrderNo(o.getOrderNo());
        cashier.setOperator(TenantContext.username());
        cashierMapper.insert(cashier);

        BigDecimal refund = c.getDeposit().subtract(depositDeduct);
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            FinDeposit d = new FinDeposit();
            d.setTenantId(o.getTenantId());
            d.setCheckinId(c.getId());
            d.setAmount(refund);
            d.setType("REFUND");
            d.setPayType(c.getDepositPayType());
            d.setOperator(TenantContext.username());
            depositMapper.insert(d);
        }

        String from = o.getStatus();
        o.setStatus("CHECKED_OUT");
        orderMapper.updateById(o);
        c.setStatus("CHECKED_OUT");
        checkinMapper.updateById(c);

        Room room = roomMapper.selectById(c.getRoomId());
        if (room != null) {
            updateRoomStatus(o.getTenantId(), room, "DIRTY", "退房待清扫");
            HkTask task = new HkTask();
            task.setTenantId(o.getTenantId());
            task.setStoreId(o.getStoreId());
            task.setRoomId(room.getId());
            task.setType("CHECKOUT");
            task.setStatus("PENDING");
            task.setNote("退房清扫：" + o.getOrderNo());
            taskMapper.insert(task);
        }
        statusLog(o.getTenantId(), o.getId(), from, "CHECKED_OUT", TenantContext.username(), "退房结算");

        if (o.getMemberId() != null) {
            MemMember m = memberMapper.selectById(o.getMemberId());
            if (m != null) {
                int points = total.multiply(new BigDecimal("1")).intValue();
                m.setPoints((m.getPoints() == null ? 0 : m.getPoints()) + points);
                m.setTotalAmount((m.getTotalAmount() == null ? BigDecimal.ZERO : m.getTotalAmount()).add(total));
                m.setBalance((m.getBalance() == null ? BigDecimal.ZERO : m.getBalance()).add(total.multiply(new BigDecimal("0.02"))));
                memberMapper.updateById(m);
                MemPointLog log = new MemPointLog();
                log.setTenantId(o.getTenantId());
                log.setMemberId(m.getId());
                log.setChangeValue(points);
                log.setReason("退房消费赠送积分");
                pointLogMapper.insert(log);
            }
        }
        notify(o.getTenantId(), "退房提醒", o.getGuestName() + " 已退房，账单金额 ¥" + total);
        return s;
    }

    @Transactional
    public void changeRoom(Long orderId, OrderDtos.ChangeRoomReq req) {
        Order o = mustGet(orderId);
        if (!"CHECKED_IN".equals(o.getStatus())) throw new BizException("仅住中订单可换房");
        Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getOrderId, orderId).eq(Checkin::getStatus, "IN_HOUSE").last("limit 1"));
        if (c == null) throw new BizException("未找到在住登记");
        Room newRoom = roomMapper.selectById(req.getRoomId());
        if (newRoom == null || !newRoom.getTenantId().equals(o.getTenantId())) throw new BizException("房间不存在");
        if (!"VACANT".equals(newRoom.getStatus()) && !"DIRTY".equals(newRoom.getStatus())) {
            throw new BizException("目标房间不可用，请选择空房");
        }
        Room oldRoom = roomMapper.selectById(c.getRoomId());
        String oldValue = oldRoom == null ? "" : oldRoom.getRoomNo();
        Long oldRoomId = c.getRoomId();
        c.setRoomId(newRoom.getId());
        checkinMapper.updateById(c);
        o.setRoomId(newRoom.getId());
        orderMapper.updateById(o);
        if (oldRoom != null) updateRoomStatus(o.getTenantId(), oldRoom, "DIRTY", "换房释放");
        updateRoomStatus(o.getTenantId(), newRoom, "OCCUPIED", "换房入住");

        StayChange change = new StayChange();
        change.setTenantId(o.getTenantId());
        change.setCheckinId(c.getId());
        change.setType("CHANGE_ROOM");
        change.setOldValue(oldValue);
        change.setNewValue(newRoom.getRoomNo());
        change.setOperator(TenantContext.username());
        stayChangeMapper.insert(change);

        HkTask task = new HkTask();
        task.setTenantId(o.getTenantId());
        task.setStoreId(o.getStoreId());
        task.setRoomId(oldRoomId);
        task.setType("CHECKOUT");
        task.setStatus("PENDING");
        task.setNote("换房清扫：" + o.getOrderNo());
        taskMapper.insert(task);
    }

    @Transactional
    public void extend(Long orderId, OrderDtos.ExtendReq req) {
        Order o = mustGet(orderId);
        if (!"CHECKED_IN".equals(o.getStatus())) throw new BizException("仅住中订单可续住");
        if (req.getNewCheckOut() == null || !req.getNewCheckOut().isAfter(o.getCheckOut())) {
            throw new BizException("续住日期需晚于原离店日期");
        }
        Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getOrderId, orderId).eq(Checkin::getStatus, "IN_HOUSE").last("limit 1"));
        if (c != null) {
            if (!roomAvailable(o.getTenantId(), c.getRoomId(), o.getCheckOut(), req.getNewCheckOut(), o.getId())) {
                throw new BizException("房间在续住日期已被占用");
            }
            c.setExpectCheckOut(req.getNewCheckOut().atTime(12, 0));
            checkinMapper.updateById(c);
            StayChange change = new StayChange();
            change.setTenantId(o.getTenantId());
            change.setCheckinId(c.getId());
            change.setType("EXTEND");
            change.setOldValue(o.getCheckOut().toString());
            change.setNewValue(req.getNewCheckOut().toString());
            change.setOperator(TenantContext.username());
            stayChangeMapper.insert(change);
        }
        o.setCheckOut(req.getNewCheckOut());
        o.setNights((int) (o.getCheckOut().toEpochDay() - o.getCheckIn().toEpochDay()));
        o.setAmount(calcAmount(o.getTenantId(), o.getRoomTypeId(), o.getCheckIn(), o.getCheckOut(), o.getRoomCount()));
        orderMapper.updateById(o);
    }

    @Transactional
    public FinDeposit deposit(Long orderId, OrderDtos.DepositReq req) {
        Order o = mustGet(orderId);
        Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getOrderId, orderId).eq(Checkin::getStatus, "IN_HOUSE").last("limit 1"));
        if (c == null) throw new BizException("未找到在住登记");
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        if ("REFUND".equals(req.getType())) {
            BigDecimal collected = depositMapper.selectList(new LambdaQueryWrapper<FinDeposit>()
                            .eq(FinDeposit::getCheckinId, c.getId())).stream()
                    .map(d -> "REFUND".equals(d.getType()) ? d.getAmount().negate() : d.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (req.getAmount().compareTo(collected) > 0) throw new BizException("退还金额不能超过实收押金");
        }
        FinDeposit d = new FinDeposit();
        d.setTenantId(o.getTenantId());
        d.setCheckinId(c.getId());
        d.setAmount(req.getAmount());
        d.setType(req.getType() == null ? "COLLECT" : req.getType());
        d.setPayType(req.getPayType());
        d.setOperator(TenantContext.username());
        depositMapper.insert(d);
        return d;
    }

    public Order mustGet(Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null && !TenantContext.isPlatform()) {
            // 租户插件已过滤掉跨租户数据；为满足越权审计口径，显式确认是否存在跨租户记录
            Order crossTenant = TenantContext.runAsPlatform(() -> orderMapper.selectById(id));
            if (crossTenant != null) {
                try {
                    jdbc.update("INSERT INTO sys_audit_log(tenant_id,user_id,username,module,action,target,ip,detail) "
                                    + "VALUES(?,?,?,?,?,?,?,?)",
                            TenantContext.tenantId(), TenantContext.userId(), TenantContext.username(),
                            "booking", "GET /api/admin/orders/{id}", "/api/admin/orders/" + id, "",
                            "status=403;error=跨租户订单访问被拦截");
                } catch (Exception ignored) {
                }
                throw new BizException(403, "越权访问已被拦截");
            }
        }
        if (o == null) throw new BizException(404, "订单不存在");
        if (!TenantContext.isPlatform() && !o.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(403, "越权访问已被拦截");
        }
        return o;
    }

    private void validateCreate(OrderDtos.CreateReq req) {
        if (req.getGuestName() == null || req.getGuestName().isBlank()) throw new BizException("入住人姓名不能为空");
        if (req.getRoomTypeId() == null) throw new BizException("请选择房型");
        if (req.getCheckIn() == null || req.getCheckOut() == null) throw new BizException("请选择入住与离店日期");
        if (!req.getCheckOut().isAfter(req.getCheckIn())) throw new BizException("离店日期必须晚于入住日期");
        if (req.getCheckIn().isBefore(LocalDate.now())) throw new BizException("入住日期不能早于今天");
    }

    private void releaseRoom(Order o) {
        if (o.getRoomId() == null) return;
        Long other = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getRoomId, o.getRoomId())
                .ne(Order::getId, o.getId())
                .in(Order::getStatus, "CONFIRMED", "CHECKED_IN"));
        if (other == null || other == 0) {
            Room room = roomMapper.selectById(o.getRoomId());
            if (room != null && "RESERVED".equals(room.getStatus())) {
                updateRoomStatus(o.getTenantId(), room, "VACANT", "订单取消释放");
            }
        }
    }

    private void updateRoomStatus(Long tid, Room room, String status, String remark) {
        RoomStatusLog log = new RoomStatusLog();
        log.setTenantId(tid);
        log.setRoomId(room.getId());
        log.setOldStatus(room.getStatus());
        log.setNewStatus(status);
        log.setOperator(TenantContext.username());
        log.setRemark(remark);
        statusLogMapper.insert(log);
        room.setStatus(status);
        roomMapper.updateById(room);
    }

    private void statusLog(Long tid, Long orderId, String from, String to, String operator, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setTenantId(tid);
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setRemark(remark);
        orderStatusLogMapper.insert(log);
    }

    private void notify(Long tid, String title, String content) {
        Notification n = new Notification();
        n.setTenantId(tid);
        n.setTitle(title);
        n.setContent(content);
        n.setChannel("SITE");
        n.setStatus("SENT");
        notificationMapper.insert(n);
    }

    private String paramValue(String key, String def) {
        // 平台参数走独立查询，避免租户插件影响
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT param_value FROM sys_param WHERE param_key=? LIMIT 1", key);
            return rows.isEmpty() ? def : String.valueOf(rows.get(0).get("param_value"));
        } catch (Exception e) {
            return def;
        }
    }

    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    private OrderDtos.CreateReq roomFor(Order o) {
        OrderDtos.CreateReq req = new OrderDtos.CreateReq();
        req.setStoreId(o.getStoreId());
        req.setRoomTypeId(o.getRoomTypeId());
        req.setCheckIn(o.getCheckIn());
        req.setCheckOut(o.getCheckOut());
        req.setRoomCount(o.getRoomCount());
        return req;
    }
}
