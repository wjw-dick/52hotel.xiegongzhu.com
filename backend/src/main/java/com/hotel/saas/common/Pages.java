package com.hotel.saas.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class Pages {
    private Pages() {
    }

    public static <T> Page<T> of(long current, long size) {
        long c = current <= 0 ? 1 : current;
        long s = size <= 0 ? 10 : Math.min(size, 200);
        return new Page<>(c, s);
    }

    public static <T> PageResult<T> result(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
