package com.hotel.saas.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.Room;
import com.hotel.saas.domain.Store;
import com.hotel.saas.mapper.RoomMapper;
import com.hotel.saas.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreMapper storeMapper;
    private final RoomMapper roomMapper;

    @GetMapping
    public ApiResult<List<Store>> list() {
        Long tid = TenantScope.requiredTenantId();
        return ApiResult.ok(storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tid).orderByAsc(Store::getId)));
    }

    @PostMapping
    public ApiResult<Store> create(@RequestBody Store store) {
        store.setId(null);
        store.setTenantId(TenantScope.requiredTenantId());
        if (store.getStatus() == null) {
            store.setStatus(1);
        }
        storeMapper.insert(store);
        return ApiResult.ok(store);
    }

    @PutMapping("/{id}")
    public ApiResult<Store> update(@PathVariable Long id, @RequestBody Store store) {
        Store db = storeMapper.selectById(id);
        if (db == null || !db.getTenantId().equals(TenantScope.requiredTenantId())) {
            throw new BizException(404, "门店不存在");
        }
        store.setId(id);
        store.setTenantId(db.getTenantId());
        storeMapper.updateById(store);
        return ApiResult.ok(storeMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long rooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStoreId, id));
        if (rooms != null && rooms > 0) {
            throw new BizException("门店下仍有房间，请先处理房间后再删除");
        }
        storeMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/summary")
    public ApiResult<Map<String, Object>> summary(@PathVariable Long id) {
        long total = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStoreId, id));
        long occupied = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStoreId, id).eq(Room::getStatus, "OCCUPIED"));
        long dirty = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStoreId, id).eq(Room::getStatus, "DIRTY"));
        return ApiResult.ok(Map.of("roomCount", total, "occupied", occupied, "dirty", dirty,
                "occupancy", total == 0 ? 0 : Math.round(occupied * 1000.0 / total) / 10.0));
    }
}
