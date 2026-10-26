package com.hotel.saas.housekeeping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.*;
import com.hotel.saas.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/hk")
@RequiredArgsConstructor
public class HousekeepingController {
    private final HkTaskMapper taskMapper;
    private final HkRepairMapper repairMapper;
    private final HkLinenMapper linenMapper;
    private final HkComplaintMapper complaintMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final StoreMapper storeMapper;
    private final RoomStatusLogMapper statusLogMapper;

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        Long tid = TenantScope.requiredTenantId();
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid));
        long pendingTasks = taskMapper.selectCount(new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getTenantId, tid).ne(HkTask::getStatus, "DONE"));
        long repairing = repairMapper.selectCount(new LambdaQueryWrapper<HkRepair>()
                .eq(HkRepair::getTenantId, tid).ne(HkRepair::getStatus, "DONE"));
        long complaints = complaintMapper.selectCount(new LambdaQueryWrapper<HkComplaint>()
                .eq(HkComplaint::getTenantId, tid).ne(HkComplaint::getStatus, "DONE"));
        Map<String, Long> byStatus = rooms.stream()
                .collect(Collectors.groupingBy(Room::getStatus, LinkedHashMap::new, Collectors.counting()));
        return ApiResult.ok(Map.of("roomTotal", rooms.size(), "dirty", byStatus.getOrDefault("DIRTY", 0L),
                "vacant", byStatus.getOrDefault("VACANT", 0L), "pendingTasks", pendingTasks,
                "repairing", repairing, "complaints", complaints, "status", byStatus));
    }

    @GetMapping("/tasks")
    public ApiResult<List<Map<String, Object>>> tasks(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String type) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<HkTask> w = new LambdaQueryWrapper<HkTask>()
                .eq(HkTask::getTenantId, tid).orderByDesc(HkTask::getId);
        if (status != null && !status.isBlank()) w.eq(HkTask::getStatus, status);
        if (type != null && !type.isBlank()) w.eq(HkTask::getType, type);
        List<HkTask> tasks = taskMapper.selectList(w);
        Map<Long, Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid))
                .stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, String> types = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getTenantId, tid)).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, String> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getTenantId, tid)).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        return ApiResult.ok(tasks.stream().map(t -> {
            Room r = rooms.get(t.getRoomId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("storeName", stores.getOrDefault(t.getStoreId(), ""));
            m.put("roomNo", r == null ? "" : r.getRoomNo());
            m.put("roomTypeName", r == null ? "" : types.getOrDefault(r.getRoomTypeId(), ""));
            m.put("type", t.getType());
            m.put("status", t.getStatus());
            m.put("assignee", t.getAssignee());
            m.put("note", t.getNote());
            m.put("finishTime", t.getFinishTime());
            m.put("createTime", t.getCreateTime());
            return m;
        }).toList());
    }

    @Data
    public static class TaskReq {
        private Long roomId;
        private Long storeId;
        private String type = "CONTINUE";
        private String assignee;
        private String note;
    }

    @PostMapping("/tasks")
    public ApiResult<HkTask> createTask(@RequestBody TaskReq req) {
        Long tid = TenantScope.requiredTenantId();
        Room room = roomMapper.selectById(req.getRoomId());
        if (room == null) throw new BizException(404, "房间不存在");
        HkTask t = new HkTask();
        t.setTenantId(tid);
        t.setStoreId(req.getStoreId() == null ? room.getStoreId() : req.getStoreId());
        t.setRoomId(req.getRoomId());
        t.setType(req.getType());
        t.setStatus("PENDING");
        t.setAssignee(req.getAssignee());
        t.setNote(req.getNote());
        taskMapper.insert(t);
        return ApiResult.ok(t);
    }

    @PostMapping("/tasks/{id}/status")
    public ApiResult<HkTask> taskStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        HkTask t = taskMapper.selectById(id);
        if (t == null) throw new BizException(404, "任务不存在");
        String status = body.get("status");
        if (!Set.of("PENDING", "PROCESSING", "DONE").contains(status)) throw new BizException("任务状态不合法");
        t.setStatus(status);
        if ("DONE".equals(status)) {
            t.setFinishTime(LocalDateTime.now());
            if (body.get("markClean") != null && !"false".equals(body.get("markClean"))) {
                cleanRoom(t.getRoomId(), "清扫完成，脏房转净房");
            }
        }
        taskMapper.updateById(t);
        return ApiResult.ok(t);
    }

    @PostMapping("/rooms/{roomId}/clean")
    public ApiResult<Void> clean(@PathVariable Long roomId, @RequestBody(required = false) Map<String, String> body) {
        cleanRoom(roomId, body == null ? "房态上报：脏房转净房" : body.getOrDefault("remark", "房态上报"));
        return ApiResult.ok();
    }

    private void cleanRoom(Long roomId, String remark) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException(404, "房间不存在");
        RoomStatusLog log = new RoomStatusLog();
        log.setTenantId(room.getTenantId());
        log.setRoomId(roomId);
        log.setOldStatus(room.getStatus());
        log.setNewStatus("VACANT");
        log.setOperator(TenantContext.username());
        log.setRemark(remark);
        statusLogMapper.insert(log);
        room.setStatus("VACANT");
        roomMapper.updateById(room);
    }

    @GetMapping("/repairs")
    public ApiResult<List<Map<String, Object>>> repairs(@RequestParam(required = false) String status) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<HkRepair> w = new LambdaQueryWrapper<HkRepair>()
                .eq(HkRepair::getTenantId, tid).orderByDesc(HkRepair::getId);
        if (status != null && !status.isBlank()) w.eq(HkRepair::getStatus, status);
        Map<Long, Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getTenantId, tid))
                .stream().collect(Collectors.toMap(Room::getId, r -> r));
        return ApiResult.ok(repairMapper.selectList(w).stream().map(r -> {
            Room room = rooms.get(r.getRoomId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("roomId", r.getRoomId());
            m.put("roomNo", room == null ? "" : room.getRoomNo());
            m.put("title", r.getTitle());
            m.put("images", r.getImages());
            m.put("status", r.getStatus());
            m.put("assignee", r.getAssignee());
            m.put("reportUser", r.getReportUser());
            m.put("finishTime", r.getFinishTime());
            m.put("createTime", r.getCreateTime());
            return m;
        }).toList());
    }

    @Data
    public static class RepairReq {
        private Long roomId;
        private Long storeId;
        private String title;
        private String images;
        private String assignee;
    }

    @PostMapping("/repairs")
    public ApiResult<HkRepair> createRepair(@RequestBody RepairReq req) {
        Long tid = TenantScope.requiredTenantId();
        Room room = roomMapper.selectById(req.getRoomId());
        if (room == null) throw new BizException(404, "房间不存在");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new BizException("请填写报修内容");
        HkRepair r = new HkRepair();
        r.setTenantId(tid);
        r.setStoreId(req.getStoreId() == null ? room.getStoreId() : req.getStoreId());
        r.setRoomId(req.getRoomId());
        r.setTitle(req.getTitle());
        r.setImages(req.getImages());
        r.setStatus("REPORTED");
        r.setAssignee(req.getAssignee());
        r.setReportUser(TenantContext.username());
        repairMapper.insert(r);
        RoomStatusLog log = new RoomStatusLog();
        log.setTenantId(tid);
        log.setRoomId(room.getId());
        log.setOldStatus(room.getStatus());
        log.setNewStatus("REPAIR");
        log.setOperator(TenantContext.username());
        log.setRemark("报修：" + req.getTitle());
        statusLogMapper.insert(log);
        room.setStatus("REPAIR");
        roomMapper.updateById(room);
        return ApiResult.ok(r);
    }

    @PostMapping("/repairs/{id}/status")
    public ApiResult<HkRepair> repairStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        HkRepair r = repairMapper.selectById(id);
        if (r == null) throw new BizException(404, "工单不存在");
        String status = body.get("status");
        if (!Set.of("REPORTED", "ASSIGNED", "REPAIRING", "ACCEPTING", "DONE").contains(status)) {
            throw new BizException("工单状态不合法");
        }
        r.setStatus(status);
        if (body.get("assignee") != null) r.setAssignee(body.get("assignee"));
        if ("DONE".equals(status)) {
            r.setFinishTime(LocalDateTime.now());
            Room room = roomMapper.selectById(r.getRoomId());
            if (room != null && "REPAIR".equals(room.getStatus())) {
                cleanRoom(room.getId(), "维修完成，房间恢复为空房");
            }
        }
        repairMapper.updateById(r);
        return ApiResult.ok(r);
    }

    @GetMapping("/linen")
    public ApiResult<List<HkLinen>> linen() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(linenMapper.selectList(new LambdaQueryWrapper<HkLinen>()
                .eq(HkLinen::getTenantId, tid).orderByDesc(HkLinen::getId).last("limit 200")));
    }

    @Data
    public static class LinenReq {
        private Long storeId;
        private String type = "TAKE";
        private String item;
        private Integer quantity;
    }

    @PostMapping("/linen")
    public ApiResult<HkLinen> createLinen(@RequestBody LinenReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getItem() == null || req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new BizException("请填写布草名称与数量");
        }
        HkLinen l = new HkLinen();
        l.setTenantId(tid);
        l.setStoreId(req.getStoreId() == null ? 1L : req.getStoreId());
        l.setType(req.getType());
        l.setItem(req.getItem());
        l.setQuantity(req.getQuantity());
        l.setOperator(TenantContext.username());
        linenMapper.insert(l);
        return ApiResult.ok(l);
    }

    @GetMapping("/complaints")
    public ApiResult<List<HkComplaint>> complaints(@RequestParam(required = false) String status) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<HkComplaint> w = new LambdaQueryWrapper<HkComplaint>()
                .eq(HkComplaint::getTenantId, tid).orderByDesc(HkComplaint::getId);
        if (status != null && !status.isBlank()) w.eq(HkComplaint::getStatus, status);
        return ApiResult.ok(complaintMapper.selectList(w));
    }

    @Data
    public static class ComplaintReq {
        private Long storeId;
        private String roomNo;
        private String guest;
        private String content;
    }

    @PostMapping("/complaints")
    public ApiResult<HkComplaint> createComplaint(@RequestBody ComplaintReq req) {
        Long tid = TenantScope.requiredTenantId();
        if (req.getContent() == null || req.getContent().isBlank()) throw new BizException("请填写客诉内容");
        HkComplaint c = new HkComplaint();
        c.setTenantId(tid);
        c.setStoreId(req.getStoreId() == null ? 1L : req.getStoreId());
        c.setRoomNo(req.getRoomNo());
        c.setGuest(req.getGuest());
        c.setContent(req.getContent());
        c.setStatus("PENDING");
        c.setHandler(TenantContext.username());
        complaintMapper.insert(c);
        return ApiResult.ok(c);
    }

    @PostMapping("/complaints/{id}/status")
    public ApiResult<HkComplaint> complaintStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        HkComplaint c = complaintMapper.selectById(id);
        if (c == null) throw new BizException(404, "客诉不存在");
        String status = body.get("status");
        if (!Set.of("PENDING", "PROCESSING", "DONE").contains(status)) throw new BizException("状态不合法");
        c.setStatus(status);
        if (body.get("handler") != null) c.setHandler(body.get("handler"));
        complaintMapper.updateById(c);
        return ApiResult.ok(c);
    }
}
