package com.hotel.saas.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.Room;
import com.hotel.saas.domain.RoomStatusLog;
import com.hotel.saas.domain.RoomType;
import com.hotel.saas.domain.Store;
import com.hotel.saas.domain.Tenant;
import com.hotel.saas.mapper.RoomMapper;
import com.hotel.saas.mapper.RoomStatusLogMapper;
import com.hotel.saas.mapper.RoomTypeMapper;
import com.hotel.saas.mapper.StoreMapper;
import com.hotel.saas.mapper.TenantMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final StoreMapper storeMapper;
    private final RoomStatusLogMapper statusLogMapper;
    private final TenantMapper tenantMapper;

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) Long storeId,
                                                      @RequestParam(required = false) Long roomTypeId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String keyword) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<Room> w = new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid)
                .orderByAsc(Room::getStoreId).orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo);
        if (storeId != null) w.eq(Room::getStoreId, storeId);
        if (roomTypeId != null) w.eq(Room::getRoomTypeId, roomTypeId);
        if (status != null && !status.isBlank()) w.eq(Room::getStatus, status);
        if (keyword != null && !keyword.isBlank()) w.like(Room::getRoomNo, keyword);
        List<Room> rooms = roomMapper.selectList(w);
        Map<Long, String> typeNames = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, String> storeNames = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        List<Map<String, Object>> rows = rooms.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("storeId", r.getStoreId());
            m.put("storeName", storeNames.getOrDefault(r.getStoreId(), ""));
            m.put("roomTypeId", r.getRoomTypeId());
            m.put("roomTypeName", typeNames.getOrDefault(r.getRoomTypeId(), ""));
            m.put("buildingId", r.getBuildingId());
            m.put("floor", r.getFloor());
            m.put("roomNo", r.getRoomNo());
            m.put("status", r.getStatus());
            m.put("lockCode", r.getLockCode());
            m.put("remark", r.getRemark());
            return m;
        }).toList();
        return ApiResult.ok(rows);
    }

    @PostMapping
    public ApiResult<Room> create(@RequestBody Room room) {
        room.setId(null);
        Long tid = TenantScope.requiredTenantId();
        room.setTenantId(tid);
        Tenant tenant = tenantMapper.selectById(tid);
        if (tenant != null && tenant.getMaxRooms() != null) {
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
            if (count != null && count >= tenant.getMaxRooms()) {
                throw new BizException("房间数已达租户配额上限（" + tenant.getMaxRooms() + "），请联系平台扩容");
            }
        }
        validate(room);
        if (room.getStatus() == null) room.setStatus("VACANT");
        roomMapper.insert(room);
        return ApiResult.ok(room);
    }

    @PutMapping("/{id}")
    public ApiResult<Room> update(@PathVariable Long id, @RequestBody Room room) {
        Room db = roomMapper.selectById(id);
        if (db == null || !db.getTenantId().equals(TenantScope.requiredTenantId())) {
            throw new BizException(404, "房间不存在");
        }
        room.setId(id);
        room.setTenantId(db.getTenantId());
        validate(room);
        roomMapper.updateById(room);
        return ApiResult.ok(roomMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Room db = roomMapper.selectById(id);
        if (db != null && ("OCCUPIED".equals(db.getStatus()) || "RESERVED".equals(db.getStatus()))) {
            throw new BizException("房间在住或已预订，不能删除");
        }
        roomMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/status")
    public ApiResult<Room> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Room db = roomMapper.selectById(id);
        if (db == null) throw new BizException(404, "房间不存在");
        String status = body.get("status");
        if (!Set.of("VACANT", "RESERVED", "OCCUPIED", "DIRTY", "REPAIR", "CLOSED").contains(status)) {
            throw new BizException("房态不合法");
        }
        RoomStatusLog log = new RoomStatusLog();
        log.setTenantId(db.getTenantId());
        log.setRoomId(id);
        log.setOldStatus(db.getStatus());
        log.setNewStatus(status);
        log.setOperator(TenantContext.username());
        log.setRemark(body.getOrDefault("remark", ""));
        statusLogMapper.insert(log);
        db.setStatus(status);
        roomMapper.updateById(db);
        return ApiResult.ok(db);
    }

    @GetMapping("/{id}/status-logs")
    public ApiResult<List<RoomStatusLog>> logs(@PathVariable Long id) {
        return ApiResult.ok(statusLogMapper.selectList(new LambdaQueryWrapper<RoomStatusLog>()
                .eq(RoomStatusLog::getRoomId, id).orderByDesc(RoomStatusLog::getCreateTime).last("limit 50")));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        Long tid = TenantScope.requiredTenantId();
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=rooms.csv");
        response.getWriter().write('\ufeff');
        PrintWriter writer = response.getWriter();
        writer.println("门店ID,房型ID,楼层,房间号,房态,备注");
        for (Room r : rooms) {
            writer.println(r.getStoreId() + "," + r.getRoomTypeId() + "," + r.getFloor() + ","
                    + r.getRoomNo() + "," + r.getStatus() + "," + (r.getRemark() == null ? "" : r.getRemark()));
        }
        writer.flush();
    }

    @PostMapping("/import")
    public ApiResult<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        Long tid = TenantScope.requiredTenantId();
        int success = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 || line.isBlank()) continue;
                line = line.replace("\ufeff", "");
                String[] p = line.split(",");
                if (p.length < 5) {
                    errors.add("第 " + lineNo + " 行字段不完整");
                    continue;
                }
                try {
                    Room r = new Room();
                    r.setTenantId(tid);
                    r.setStoreId(Long.parseLong(p[0].trim()));
                    r.setRoomTypeId(Long.parseLong(p[1].trim()));
                    r.setFloor(Integer.parseInt(p[2].trim()));
                    r.setRoomNo(p[3].trim());
                    r.setStatus(p[4].trim().isEmpty() ? "VACANT" : p[4].trim());
                    r.setRemark(p.length > 5 ? p[5].trim() : "");
                    validate(r);
                    roomMapper.insert(r);
                    success++;
                } catch (Exception e) {
                    errors.add("第 " + lineNo + " 行：" + e.getMessage());
                }
            }
        }
        return ApiResult.ok(Map.of("success", success, "errors", errors));
    }

    private void validate(Room room) {
        if (room.getRoomNo() == null || room.getRoomNo().isBlank()) {
            throw new BizException("房间号不能为空");
        }
        if (room.getStoreId() == null || room.getRoomTypeId() == null) {
            throw new BizException("门店和房型不能为空");
        }
        Long dup = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getTenantId, room.getTenantId())
                .eq(Room::getStoreId, room.getStoreId())
                .eq(Room::getRoomNo, room.getRoomNo())
                .ne(room.getId() != null, Room::getId, room.getId()));
        if (dup != null && dup > 0) {
            throw new BizException("同一门店下房间号不能重复");
        }
    }
}
