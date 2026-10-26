package com.hotel.saas.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.Room;
import com.hotel.saas.domain.RoomType;
import com.hotel.saas.mapper.RoomMapper;
import com.hotel.saas.mapper.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/room-types")
@RequiredArgsConstructor
public class RoomTypeController {
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;

    @GetMapping
    public ApiResult<List<RoomType>> list(@RequestParam(required = false) Long storeId) {
        Long tid = TenantScope.requiredTenantId();
        LambdaQueryWrapper<RoomType> w = new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getTenantId, tid).orderByAsc(RoomType::getSort);
        if (storeId != null) {
            w.eq(RoomType::getStoreId, storeId);
        }
        return ApiResult.ok(roomTypeMapper.selectList(w));
    }

    @PostMapping
    public ApiResult<RoomType> create(@RequestBody RoomType roomType) {
        roomType.setId(null);
        roomType.setTenantId(TenantScope.requiredTenantId());
        if (roomType.getStatus() == null) {
            roomType.setStatus(1);
        }
        roomTypeMapper.insert(roomType);
        return ApiResult.ok(roomType);
    }

    @PutMapping("/{id}")
    public ApiResult<RoomType> update(@PathVariable Long id, @RequestBody RoomType roomType) {
        RoomType db = roomTypeMapper.selectById(id);
        if (db == null || !db.getTenantId().equals(TenantScope.requiredTenantId())) {
            throw new BizException(404, "房型不存在");
        }
        roomType.setId(id);
        roomType.setTenantId(db.getTenantId());
        roomTypeMapper.updateById(roomType);
        return ApiResult.ok(roomTypeMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long rooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomTypeId, id));
        if (rooms != null && rooms > 0) {
            throw new BizException("房型下仍有房间，不能删除");
        }
        roomTypeMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/detail")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        RoomType rt = roomTypeMapper.selectById(id);
        if (rt == null) {
            throw new BizException(404, "房型不存在");
        }
        long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomTypeId, id));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roomType", rt);
        m.put("roomCount", roomCount);
        return ApiResult.ok(m);
    }
}
