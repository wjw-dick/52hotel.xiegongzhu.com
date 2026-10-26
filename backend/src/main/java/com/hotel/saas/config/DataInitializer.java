package com.hotel.saas.config;

import com.hotel.saas.common.TenantContext;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final TenantMapper tenantMapper;
    private final PlatformUserMapper platformUserMapper;
    private final TenantUserMapper tenantUserMapper;
    private final RoleMapper roleMapper;
    private final StoreMapper storeMapper;
    private final BuildingMapper buildingMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RateMapper rateMapper;
    private final OrderMapper orderMapper;
    private final OrderGuestMapper orderGuestMapper;
    private final CheckinMapper checkinMapper;
    private final PoliceReportMapper policeReportMapper;
    private final HkTaskMapper hkTaskMapper;
    private final HkRepairMapper hkRepairMapper;
    private final HkLinenMapper hkLinenMapper;
    private final HkComplaintMapper hkComplaintMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinCashierMapper cashierMapper;
    private final FinArMapper arMapper;
    private final FinArLedgerMapper arLedgerMapper;
    private final FinShiftMapper shiftMapper;
    private final FinDepositMapper depositMapper;
    private final MemLevelMapper levelMapper;
    private final MemMemberMapper memberMapper;
    private final MemCouponMapper couponMapper;
    private final DictMapper dictMapper;
    private final ParamMapper paramMapper;
    private final LanguageMapper languageMapper;
    private final I18nMapper i18nMapper;
    private final MenuConfigMapper menuMapper;
    private final StorageConfigMapper storageMapper;
    private final WechatConfigMapper wechatMapper;
    private final NotifyTemplateMapper notifyTemplateMapper;
    private final NotificationMapper notificationMapper;
    private final ChannelMapper channelMapper;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (tenantMapper.selectCount(null) > 0) {
            log.info("演示数据已存在，跳过初始化");
            return;
        }
        TenantContext.set(new TenantContext.User(0L, "system", 0L, "PLATFORM", "PLATFORM_ADMIN", true, null));
        try {
            seedBase();
            Long t1 = seedTenant("YQ-HZ", "云栖民宿（杭州）", "HOMESTAY", "杭州", 30, 80);
            Long t2 = seedTenant("XH-SH", "星河酒店（上海）", "HOTEL", "上海", 50, 160);
            seedTenantData(t1, "YQ-HZ", "云栖·西湖民宿", "云栖·莫干山山居", "杭州", true);
            seedTenantData(t2, "XH-SH", "星河·外滩酒店", null, "上海", false);
            log.info("演示数据初始化完成：平台账号 admin/admin123；租户账号 admin/admin123（YQ-HZ、XH-SH）");
        } finally {
            TenantContext.clear();
        }
    }

    private void seedBase() {
        PlatformUser admin = new PlatformUser();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setRealName("平台超级管理员");
        admin.setRoleCode("PLATFORM_ADMIN");
        admin.setStatus(1);
        platformUserMapper.insert(admin);

        String[][] dicts = {
                {"room_type", "STANDARD", "标准间"}, {"room_type", "BIG_BED", "大床房"},
                {"room_type", "TWIN", "双床房"}, {"room_type", "FAMILY", "家庭房"},
                {"room_type", "SUITE", "套房"}, {"room_type", "WHOLE", "包栋"},
                {"rate_type", "BAR", "门市价"}, {"rate_type", "CHANNEL", "渠道价"}, {"rate_type", "AGREEMENT", "协议价"},
                {"room_status", "VACANT", "空房"}, {"room_status", "RESERVED", "预订"}, {"room_status", "OCCUPIED", "在住"},
                {"room_status", "DIRTY", "脏房"}, {"room_status", "REPAIR", "维修"}, {"room_status", "CLOSED", "关闭"},
                {"pay_type", "WECHAT", "微信支付"}, {"pay_type", "ALIPAY", "支付宝"}, {"pay_type", "CASH", "现金"},
                {"pay_type", "POS", "POS刷卡"}, {"pay_type", "ARRIVAL", "到店付"},
                {"channel", "CTRIP", "携程"}, {"channel", "MEITUAN", "美团"}, {"channel", "FLIGGY", "飞猪"},
                {"channel", "TUIJIA", "途家"}, {"channel", "DOUYIN", "抖音"},
                {"id_type", "ID_CARD", "居民身份证"}, {"id_type", "PASSPORT", "护照"}, {"id_type", "HK_MACAU", "港澳通行证"},
                {"complaint_type", "NOISE", "噪音"}, {"complaint_type", "CLEAN", "卫生"}, {"complaint_type", "SERVICE", "服务"},
                {"repair_type", "ELECTRIC", "水电"}, {"repair_type", "AIRCON", "空调"}, {"repair_type", "DOOR_LOCK", "门锁"}
        };
        int i = 1;
        for (String[] d : dicts) {
            Dict dict = new Dict();
            dict.setType(d[0]);
            dict.setCode(d[1]);
            dict.setName(d[2]);
            dict.setSort(i++);
            dict.setStatus(1);
            dictMapper.insert(dict);
        }

        String[][] params = {
                {"tenant_expire_strategy", "AUTO_DISABLE", "租户到期策略"},
                {"free_cancel_hours", "24", "免费取消默认时长（小时）"},
                {"arrival_hold_hours", "18", "到店付保留默认时长（小时）"},
                {"deposit_pay_type", "WECHAT", "押金默认支付方式"},
                {"police_report_enabled", "true", "公安实名报送开关"},
                {"platform_name", "云宿 SaaS", "平台名称"}
        };
        for (String[] p : params) {
            Param param = new Param();
            param.setParamKey(p[0]);
            param.setParamValue(p[1]);
            param.setName(p[2]);
            param.setScope("PLATFORM");
            paramMapper.insert(param);
        }

        Language zh = new Language();
        zh.setCode("zh-CN");
        zh.setName("简体中文");
        zh.setEnabled(1);
        zh.setSort(1);
        languageMapper.insert(zh);
        Language en = new Language();
        en.setCode("en");
        en.setName("English");
        en.setEnabled(1);
        en.setSort(2);
        languageMapper.insert(en);
    }

    private Long seedTenant(String code, String name, String type, String city, int maxUsers, int maxRooms) {
        Tenant t = new Tenant();
        t.setCode(code);
        t.setName(name);
        t.setType(type);
        t.setStatus("NORMAL");
        t.setContact("运营负责人");
        t.setPhone("400-800-" + (1000 + maxRooms));
        t.setOpenTime(LocalDate.now().minusMonths(2));
        t.setExpireTime(LocalDate.now().plusYears(1));
        t.setMaxUsers(maxUsers);
        t.setMaxRooms(maxRooms);
        t.setRemark("演示租户");
        tenantMapper.insert(t);
        Long tid = t.getId();

        String[][] users = {
                {"admin", "admin123", "租户管理员", "TENANT_ADMIN"},
                {"front", "front123", "前台接待", "FRONT_DESK"},
                {"house", "house123", "客房管家", "HOUSEKEEPING"},
                {"finance", "finance123", "财务收银", "FINANCE"}
        };
        for (String[] u : users) {
            TenantUser tu = new TenantUser();
            tu.setTenantId(tid);
            tu.setUsername(u[0]);
            tu.setPassword(encoder.encode(u[1]));
            tu.setRealName(u[2]);
            tu.setPhone("1380000" + (1000 + maxRooms));
            tu.setRoleCode(u[3]);
            tu.setStatus(1);
            tenantUserMapper.insert(tu);
        }
        String[][] roles = {
                {"TENANT_ADMIN", "租户管理员", "全部模块"},
                {"STORE_MANAGER", "店长/值班经理", "门店运营,报表"},
                {"FRONT_DESK", "前台/客房管家", "预订,入住,房态,收银"},
                {"HOUSEKEEPING", "保洁/客房服务", "房务,报修,布草"},
                {"FINANCE", "财务/收银", "结算,挂账,报表"},
                {"OTA_OPERATOR", "OTA运营", "渠道,房价"}
        };
        for (String[] r : roles) {
            Role role = new Role();
            role.setTenantId(tid);
            role.setCode(r[0]);
            role.setName(r[1]);
            role.setPermissions(r[2]);
            role.setStatus(1);
            roleMapper.insert(role);
        }
        return tid;
    }

    private void seedTenantData(Long tid, String tenantCode, String storeName, String secondStore, String city, boolean rich) {
        Store store = new Store();
        store.setTenantId(tid);
        store.setName(storeName);
        store.setCode(tenantCode + "-01");
        store.setCity(city);
        store.setAddress(city + "市西湖区云栖路 88 号");
        store.setPhone("0571-88886666");
        store.setIntro("藏在山水之间的品质住宿，推窗见景，慢下来感受城市之外的呼吸。");
        store.setStatus(1);
        storeMapper.insert(store);

        Building building = new Building();
        building.setTenantId(tid);
        building.setStoreId(store.getId());
        building.setName("主楼");
        building.setFloors(3);
        building.setSort(1);
        buildingMapper.insert(building);

        Long store2 = null;
        if (secondStore != null) {
            Store s2 = new Store();
            s2.setTenantId(tid);
            s2.setName(secondStore);
            s2.setCode(tenantCode + "-02");
            s2.setCity("湖州");
            s2.setAddress("湖州市德清县莫干山镇 66 号");
            s2.setPhone("0572-77776666");
            s2.setIntro("竹海深处的山居院落，适合家庭度假与小型团建。");
            s2.setStatus(1);
            storeMapper.insert(s2);
            store2 = s2.getId();
        }

        String[][] types = {
                {"云栖大床房", "BIG_BED", "32", "2", "1.8m 大床", "独立卫浴,智能马桶,投影,免费WiFi", "680"},
                {"山景双床房", "TWIN", "38", "2", "1.2m 双床", "落地窗,浴缸,迷你吧,免费WiFi", "880"},
                {"亲子家庭套房", "FAMILY", "55", "4", "1.8m+1.2m", "儿童帐篷,浴缸,冰箱,免费WiFi", "1280"}
        };
        Long[] typeIds = new Long[types.length];
        for (int i = 0; i < types.length; i++) {
            RoomType rt = new RoomType();
            rt.setTenantId(tid);
            rt.setStoreId(store.getId());
            rt.setName(types[i][0]);
            rt.setCode(tenantCode + "-RT0" + (i + 1));
            rt.setArea(new BigDecimal(types[i][2]));
            rt.setCapacity(Integer.parseInt(types[i][3]));
            rt.setBedType(types[i][4]);
            rt.setFacilities(types[i][5]);
            rt.setBasePrice(new BigDecimal(types[i][6]));
            rt.setIntro(types[i][0] + "，面积 " + types[i][2] + "㎡，可住 " + types[i][3] + " 人，" + types[i][5] + "。");
            rt.setStatus(1);
            rt.setSort(i + 1);
            roomTypeMapper.insert(rt);
            typeIds[i] = rt.getId();
        }

        String[] statuses = {"VACANT", "VACANT", "OCCUPIED", "DIRTY", "VACANT", "REPAIR"};
        String[] suffixes = {"01", "02", "03", "04", "05", "06"};
        for (int i = 0; i < 6; i++) {
            Room room = new Room();
            room.setTenantId(tid);
            room.setStoreId(store.getId());
            room.setBuildingId(building.getId());
            room.setFloor(i / 2 + 1);
            room.setRoomNo((i / 2 + 1) + "0" + (i % 2 + 1));
            room.setRoomTypeId(typeIds[i % typeIds.length]);
            room.setStatus(statuses[i]);
            room.setLockCode("LC-" + tenantCode + "-" + suffixes[i]);
            room.setRemark("");
            roomMapper.insert(room);
        }

        if (secondStore != null) {
            RoomType rt = new RoomType();
            rt.setTenantId(tid);
            rt.setStoreId(store2);
            rt.setName("山居庭院房");
            rt.setCode(tenantCode + "-RT09");
            rt.setArea(new BigDecimal("45"));
            rt.setCapacity(2);
            rt.setBedType("1.8m 大床");
            rt.setFacilities("私享庭院,壁炉,浴缸,免费WiFi");
            rt.setBasePrice(new BigDecimal("980"));
            rt.setIntro("推门即庭院，适合二人度假。");
            rt.setStatus(1);
            rt.setSort(1);
            roomTypeMapper.insert(rt);
            for (int i = 1; i <= 4; i++) {
                Room room = new Room();
                room.setTenantId(tid);
                room.setStoreId(store2);
                room.setFloor(1);
                room.setRoomNo("A0" + i);
                room.setRoomTypeId(rt.getId());
                room.setStatus("VACANT");
                roomMapper.insert(room);
            }
        }

        for (Long typeId : typeIds) {
            for (int d = 0; d < 21; d++) {
                LocalDate date = LocalDate.now().plusDays(d);
                Rate rate = new Rate();
                rate.setTenantId(tid);
                rate.setRoomTypeId(typeId);
                rate.setRateDate(date);
                rate.setPriceType("BAR");
                BigDecimal base = new BigDecimal("680").add(new BigDecimal(typeId % 3 * 100));
                if (date.getDayOfWeek().getValue() >= 6) {
                    base = base.add(new BigDecimal("120"));
                }
                rate.setPrice(base);
                rateMapper.insert(rate);
            }
        }

        String[] levelNames = {"银卡会员", "金卡会员", "钻石会员"};
        BigDecimal[] mins = {BigDecimal.ZERO, new BigDecimal("2000"), new BigDecimal("8000")};
        BigDecimal[] discounts = {new BigDecimal("1.00"), new BigDecimal("0.95"), new BigDecimal("0.90")};
        Long[] levelIds = new Long[3];
        for (int i = 0; i < 3; i++) {
            MemLevel level = new MemLevel();
            level.setTenantId(tid);
            level.setName(levelNames[i]);
            level.setMinAmount(mins[i]);
            level.setDiscount(discounts[i]);
            level.setBenefits(i == 0 ? "积分累积" : i == 1 ? "积分累积,延迟退房,欢迎水果" : "积分累积,延迟退房,免费升级,专属客服");
            level.setSort(i + 1);
            levelMapper.insert(level);
            levelIds[i] = level.getId();
        }

        String[][] members = {
                {"林知夏", "13900001001", "F", "6800", "1280"},
                {"周远", "13900001002", "M", "3200", "560"},
                {"苏禾", "13900001003", "F", "15600", "2400"},
                {"陈默", "13900001004", "M", "800", "120"}
        };
        Long[] memberIds = new Long[members.length];
        for (int i = 0; i < members.length; i++) {
            MemMember m = new MemMember();
            m.setTenantId(tid);
            m.setUnionId("union-demo-" + tenantCode + "-" + i);
            m.setOpenId("openid-demo-" + tenantCode + "-" + i);
            m.setName(members[i][0]);
            m.setPhone(members[i][1]);
            m.setGender(members[i][2]);
            m.setLevelId(levelIds[Math.min(i, 2)]);
            m.setPoints(Integer.parseInt(members[i][4]));
            m.setBalance(new BigDecimal(members[i][4]).multiply(new BigDecimal("0.1")));
            m.setTotalAmount(new BigDecimal(members[i][3]));
            m.setTags(i == 0 ? "亲子,高复购" : i == 2 ? "高价值,商务" : "新客");
            m.setStatus(1);
            memberMapper.insert(m);
            memberIds[i] = m.getId();

            MemCoupon coupon = new MemCoupon();
            coupon.setTenantId(tid);
            coupon.setMemberId(m.getId());
            coupon.setName(i % 2 == 0 ? "连住两晚立减 100 元" : "房费 9 折券");
            coupon.setAmount(i % 2 == 0 ? new BigDecimal("100") : BigDecimal.ZERO);
            coupon.setStatus("UNUSED");
            coupon.setExpireDate(LocalDate.now().plusMonths(2));
            couponMapper.insert(coupon);
        }

        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getTenantId, tid).eq(Room::getStoreId, store.getId()).orderByAsc(Room::getId));
        // 演示订单：今日到店 / 在住 / 今日离店
        Long room1 = rooms.get(0).getId();
        Long room2 = rooms.get(1).getId();
        Long room3 = rooms.get(2).getId();
        Long room4 = rooms.get(3).getId();

        Order inHouse = order(tid, store.getId(), tenantCode + "-" + LocalDate.now().getYear() + "-0001",
                "ONLINE", typeIds[0], room3, memberIds[0], "林知夏", "13900001001",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), "CHECKED_IN", "ONLINE", "携程");
        createCheckin(tid, inHouse, room3, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), "388");

        Order arrival = order(tid, store.getId(), tenantCode + "-" + LocalDate.now().getYear() + "-0002",
                "WALK_IN", typeIds[1], room1, memberIds[1], "周远", "13900001002",
                LocalDate.now(), LocalDate.now().plusDays(1), "CONFIRMED", "ARRIVAL", null);
        arrival.setRemark("预计 18:00 到店，保留至 20:00");
        orderMapper.updateById(arrival);

        Order departure = order(tid, store.getId(), tenantCode + "-" + LocalDate.now().getYear() + "-0003",
                "OTA", typeIds[2], room4, memberIds[2], "苏禾", "13900001003",
                LocalDate.now().minusDays(2), LocalDate.now(), "CHECKED_OUT", "ONLINE", "美团");
        FinSettlement settle = new FinSettlement();
        settle.setTenantId(tid);
        settle.setOrderId(departure.getId());
        settle.setRoomFee(departure.getAmount());
        settle.setDepositDeduct(BigDecimal.ZERO);
        settle.setDiscount(new BigDecimal("80"));
        settle.setOtherFee(new BigDecimal("68"));
        settle.setTotal(departure.getAmount().subtract(new BigDecimal("80")).add(new BigDecimal("68")));
        settle.setPayType("WECHAT");
        settle.setStatus("PAID");
        settle.setOperator("front");
        settlementMapper.insert(settle);
        cashier(tid, store.getId(), "ROOM", settle.getTotal(), "WECHAT", departure.getOrderNo(), "front");

        Order future = order(tid, store.getId(), tenantCode + "-" + LocalDate.now().getYear() + "-0004",
                "AGREEMENT", typeIds[0], null, memberIds[3], "陈默", "13900001004",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), "CONFIRMED", "ARRIVAL", "协议企业");
        future.setCompany("杭州云帆科技有限公司");
        orderMapper.updateById(future);

        Order waitlist = order(tid, store.getId(), tenantCode + "-" + LocalDate.now().getYear() + "-0005",
                "WAITLIST", typeIds[1], null, null, "许安", "13900001005",
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), "WAITLIST", "ARRIVAL", null);
        waitlist.setRemark("候补，待房态释放");

        HkTask task1 = new HkTask();
        task1.setTenantId(tid);
        task1.setStoreId(store.getId());
        task1.setRoomId(room4);
        task1.setType("CHECKOUT");
        task1.setStatus("PENDING");
        task1.setAssignee("house");
        task1.setNote("退房清扫，补充一次性用品");
        hkTaskMapper.insert(task1);

        HkTask task2 = new HkTask();
        task2.setTenantId(tid);
        task2.setStoreId(store.getId());
        task2.setRoomId(room1);
        task2.setType("MAINTAIN");
        task2.setStatus("DONE");
        task2.setAssignee("house");
        task2.setFinishTime(LocalDateTime.now().minusHours(2));
        hkTaskMapper.insert(task2);

        HkRepair repair = new HkRepair();
        repair.setTenantId(tid);
        repair.setStoreId(store.getId());
        repair.setRoomId(room2);
        repair.setTitle("卫生间花洒出水偏小");
        repair.setStatus("ASSIGNED");
        repair.setAssignee("工程老李");
        repair.setReportUser("house");
        hkRepairMapper.insert(repair);

        HkLinen linen = new HkLinen();
        linen.setTenantId(tid);
        linen.setStoreId(store.getId());
        linen.setType("WASH");
        linen.setItem("床单");
        linen.setQuantity(20);
        linen.setOperator("house");
        hkLinenMapper.insert(linen);

        HkComplaint complaint = new HkComplaint();
        complaint.setTenantId(tid);
        complaint.setStoreId(store.getId());
        complaint.setRoomNo(rooms.get(2).getRoomNo());
        complaint.setGuest("林女士");
        complaint.setContent("隔壁房间夜间声音较大，希望协助提醒。");
        complaint.setStatus("PROCESSING");
        complaint.setHandler("front");
        hkComplaintMapper.insert(complaint);

        FinAr ar = new FinAr();
        ar.setTenantId(tid);
        ar.setCompany("杭州云帆科技有限公司");
        ar.setContact("陈经理");
        ar.setPhone("13800009999");
        ar.setBalance(new BigDecimal("5600"));
        ar.setStatus("NORMAL");
        arMapper.insert(ar);
        FinArLedger ledger = new FinArLedger();
        ledger.setTenantId(tid);
        ledger.setArId(ar.getId());
        ledger.setType("CHARGE");
        ledger.setAmount(new BigDecimal("5600"));
        ledger.setRemark("协议客户月结挂账");
        ledger.setOperator("finance");
        arLedgerMapper.insert(ledger);

        FinShift shift = new FinShift();
        shift.setTenantId(tid);
        shift.setStoreId(store.getId());
        shift.setShiftName("早班");
        shift.setOperator("front");
        shift.setStartTime(LocalDateTime.now().withHour(8).withMinute(0));
        shift.setCashAmount(new BigDecimal("1200"));
        shift.setStatus("OPEN");
        shiftMapper.insert(shift);

        Channel ch = new Channel();
        ch.setTenantId(tid);
        ch.setName("携程");
        ch.setCode("CTRIP");
        ch.setStatus(1);
        ch.setSyncStock(1);
        ch.setLastSync(LocalDateTime.now().minusMinutes(8));
        channelMapper.insert(ch);
        Channel ch2 = new Channel();
        ch2.setTenantId(tid);
        ch2.setName("美团");
        ch2.setCode("MEITUAN");
        ch2.setStatus(1);
        ch2.setSyncStock(1);
        ch2.setLastSync(LocalDateTime.now().minusMinutes(21));
        channelMapper.insert(ch2);

        Notification n = new Notification();
        n.setTenantId(tid);
        n.setTitle("今日预抵提醒");
        n.setContent("今日有 1 间房待入住，请提前准备房卡与欢迎水果。");
        n.setChannel("SITE");
        n.setStatus("SENT");
        notificationMapper.insert(n);

        if (rich) {
            String[][] i18n = {
                    {"zh-CN", "portal.slogan", "山水之间，安心住下"},
                    {"en", "portal.slogan", "Stay between mountains and water"},
                    {"zh-CN", "booking.title", "在线预订"},
                    {"en", "booking.title", "Book Now"}
            };
            for (String[] row : i18n) {
                I18n m = new I18n();
                m.setTenantId(tid);
                m.setLang(row[0]);
                m.setMsgKey(row[1]);
                m.setMsgValue(row[2]);
                i18nMapper.insert(m);
            }
        }

        StorageConfig storage = new StorageConfig();
        storage.setTenantId(tid);
        storage.setDriver("LOCAL");
        storage.setConfigJson("{\"basePath\":\"./data/uploads\"}");
        storage.setEnabled(1);
        storageMapper.insert(storage);

        WechatConfig wx = new WechatConfig();
        wx.setTenantId(tid);
        wx.setAppType("MINI");
        wx.setAppId("wx-demo-" + tenantCode.toLowerCase());
        wx.setAppSecret("demo-secret-encrypted");
        wx.setDomain("https://h5.example.com");
        wx.setEnabled(1);
        wechatMapper.insert(wx);
        WechatConfig wx2 = new WechatConfig();
        wx2.setTenantId(tid);
        wx2.setAppType("OFFICIAL");
        wx2.setAppId("wx-official-" + tenantCode.toLowerCase());
        wx2.setAppSecret("demo-secret-encrypted");
        wx2.setToken("hotel2026");
        wx2.setAesKey("demo-aes-key");
        wx2.setDomain("https://h5.example.com");
        wx2.setEnabled(1);
        wechatMapper.insert(wx2);

        String[][] templates = {
                {"BOOKING_CONFIRM", "OFFICIAL", "预订确认：{{store}}/{{roomType}}/{{checkIn}}"},
                {"CHECKIN_REMIND", "MINI", "入住提醒：{{roomNo}} 已为您准备好"},
                {"CHECKOUT_REMIND", "OFFICIAL", "退房提醒：{{checkOut}} 前办理退房"},
                {"MEMBER_UPGRADE", "SITE", "会员升级：恭喜成为 {{level}}"},
                {"COUPON", "SITE", "优惠活动：{{name}} 有效期至 {{expire}}"}
        };
        for (String[] tp : templates) {
            NotifyTemplate tpl = new NotifyTemplate();
            tpl.setTenantId(tid);
            tpl.setBizType(tp[0]);
            tpl.setChannel(tp[1]);
            tpl.setTemplateId("TPL-" + tp[0]);
            tpl.setTitle(tp[2]);
            tpl.setContent(tp[2]);
            tpl.setEnabled(1);
            notifyTemplateMapper.insert(tpl);
        }

        seedMenus(tid);
        seedTenantConfig(tid, store.getId());
    }

    private void seedMenus(Long tid) {
        String[][] admin = {
                {"dashboard", "经营概览", "DataLine", "/dashboard", "0"},
                {"store", "门店与房源", "OfficeBuilding", "/stores", "1"},
                {"booking", "预订管理", "Tickets", "/orders", "2"},
                {"front", "前台接待", "Service", "/front-desk", "3"},
                {"housekeeping", "房务保洁", "Brush", "/housekeeping", "4"},
                {"finance", "收银财务", "Money", "/finance", "5"},
                {"member", "会员营销", "User", "/members", "6"},
                {"report", "报表中心", "TrendCharts", "/reports", "7"},
                {"system", "系统配置", "Setting", "/config", "8"}
        };
        int sort = 1;
        for (String[] m : admin) {
            MenuConfig mc = new MenuConfig();
            mc.setTenantId(tid);
            mc.setPlatform("ADMIN");
            mc.setCode(m[0]);
            mc.setTitle(m[1]);
            mc.setIcon(m[2]);
            mc.setPath(m[3]);
            mc.setSort(sort++);
            mc.setEnabled(1);
            menuMapper.insert(mc);
        }
        String[][] mini = {
                {"home", "首页", "home", "/pages/index/index"},
                {"booking", "预订", "calendar", "/pages/booking/index"},
                {"orders", "订单", "list", "/pages/orders/index"},
                {"member", "会员", "vip", "/pages/member/index"},
                {"mine", "我的", "user", "/pages/mine/index"}
        };
        sort = 1;
        for (String[] m : mini) {
            MenuConfig mc = new MenuConfig();
            mc.setTenantId(tid);
            mc.setPlatform("MINI");
            mc.setCode(m[0]);
            mc.setTitle(m[1]);
            mc.setIcon(m[2]);
            mc.setPath(m[3]);
            mc.setSort(sort++);
            mc.setEnabled(1);
            menuMapper.insert(mc);
        }
        String[][] h5 = {
                {"home", "首页", "首页", "/"},
                {"rooms", "房型", "房型", "/rooms"},
                {"booking", "预订", "预订", "/booking"},
                {"orders", "订单查询", "订单查询", "/orders"}
        };
        sort = 1;
        for (String[] m : h5) {
            MenuConfig mc = new MenuConfig();
            mc.setTenantId(tid);
            mc.setPlatform("H5");
            mc.setCode(m[0]);
            mc.setTitle(m[1]);
            mc.setIcon(m[2]);
            mc.setPath(m[3]);
            mc.setSort(sort++);
            mc.setEnabled(1);
            menuMapper.insert(mc);
        }
        String[][] app = {
                {"home", "首页", "home", "/"},
                {"orders", "订单", "orders", "/orders"},
                {"member", "会员", "member", "/member"},
                {"mine", "我的", "mine", "/mine"}
        };
        sort = 1;
        for (String[] m : app) {
            MenuConfig mc = new MenuConfig();
            mc.setTenantId(tid);
            mc.setPlatform("APP");
            mc.setCode(m[0]);
            mc.setTitle(m[1]);
            mc.setIcon(m[2]);
            mc.setPath(m[3]);
            mc.setSort(sort++);
            mc.setEnabled(1);
            menuMapper.insert(mc);
        }
    }

    private void seedTenantConfig(Long tid, Long storeId) {
        String[][] configs = {
                {"tenant_name", "云栖住宿"},
                {"logo", ""},
                {"service_phone", "400-800-8888"},
                {"wechat_pay_enabled", "true"},
                {"notify_enabled", "true"},
                {"default_store_id", String.valueOf(storeId)},
                {"slogan", "山水之间，安心住下"}
        };
        for (String[] c : configs) {
            TenantConfig tc = new TenantConfig();
            tc.setTenantId(tid);
            tc.setConfigKey(c[0]);
            tc.setConfigValue(c[1]);
            tenantConfigMapper.insert(tc);
        }
    }

    private final TenantConfigMapper tenantConfigMapper;

    private Order order(Long tid, Long storeId, String no, String source, Long typeId, Long roomId, Long memberId,
                        String guest, String phone, LocalDate in, LocalDate out, String status, String payType, String channel) {
        Order o = new Order();
        o.setTenantId(tid);
        o.setStoreId(storeId);
        o.setOrderNo(no);
        o.setSource(source);
        o.setChannel(channel);
        o.setRoomTypeId(typeId);
        o.setRoomId(roomId);
        o.setMemberId(memberId);
        o.setGuestName(guest);
        o.setGuestPhone(phone);
        o.setCheckIn(in);
        o.setCheckOut(out);
        int nights = (int) (out.toEpochDay() - in.toEpochDay());
        o.setNights(Math.max(nights, 1));
        o.setRoomCount(1);
        BigDecimal amount = new BigDecimal("680").multiply(new BigDecimal(Math.max(nights, 1)));
        o.setAmount(amount);
        o.setPaidAmount("ONLINE".equals(payType) ? amount : BigDecimal.ZERO);
        o.setStatus(status);
        o.setPayType(payType);
        o.setCreatedBy("system");
        orderMapper.insert(o);

        OrderGuest og = new OrderGuest();
        og.setTenantId(tid);
        og.setOrderId(o.getId());
        og.setName(guest);
        og.setIdType("ID_CARD");
        og.setIdNo("3301061990" + String.format("%04d", o.getId() % 10000));
        og.setPhone(phone);
        og.setIsMinor(0);
        orderGuestMapper.insert(og);
        return o;
    }

    private void createCheckin(Long tid, Order order, Long roomId, LocalDateTime in, LocalDateTime out, String deposit) {
        Checkin c = new Checkin();
        c.setTenantId(tid);
        c.setOrderId(order.getId());
        c.setStoreId(order.getStoreId());
        c.setRoomId(roomId);
        c.setGuestName(order.getGuestName());
        c.setIdNo("3301061990" + String.format("%04d", order.getId() % 10000));
        c.setCheckInTime(in);
        c.setExpectCheckOut(out);
        c.setDeposit(new BigDecimal(deposit));
        c.setDepositPayType("WECHAT");
        c.setCardNo("CARD-" + roomId);
        c.setStatus("IN_HOUSE");
        checkinMapper.insert(c);

        PoliceReport pr = new PoliceReport();
        pr.setTenantId(tid);
        pr.setCheckinId(c.getId());
        pr.setGuestName(order.getGuestName());
        pr.setIdNo(mask(c.getIdNo()));
        pr.setRoomNo("已登记");
        pr.setReportStatus("SUCCESS");
        pr.setReportTime(LocalDateTime.now().minusHours(20));
        pr.setResponse("公安旅馆业系统回执成功（演示）");
        policeReportMapper.insert(pr);

        FinDeposit d = new FinDeposit();
        d.setTenantId(tid);
        d.setCheckinId(c.getId());
        d.setAmount(new BigDecimal(deposit));
        d.setType("COLLECT");
        d.setPayType("WECHAT");
        d.setOperator("front");
        depositMapper.insert(d);
    }

    private void cashier(Long tid, Long storeId, String biz, BigDecimal amount, String pay, String orderNo, String operator) {
        FinCashier c = new FinCashier();
        c.setTenantId(tid);
        c.setStoreId(storeId);
        c.setBizType(biz);
        c.setAmount(amount);
        c.setPayType(pay);
        c.setOrderNo(orderNo);
        c.setOperator(operator);
        cashierMapper.insert(c);
    }

    private String mask(String idNo) {
        if (idNo == null || idNo.length() < 8) {
            return idNo;
        }
        return idNo.substring(0, 4) + "**********" + idNo.substring(idNo.length() - 4);
    }
}
