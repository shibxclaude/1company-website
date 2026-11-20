package com.cn01.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> list, long total, long page, long size) {
    public static <E, T> PageResult<T> of(IPage<E> p, Function<E, T> mapper) {
        return new PageResult<>(p.getRecords().stream().map(mapper).toList(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public static <T> PageResult<T> of(IPage<T> p) {
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }
}
