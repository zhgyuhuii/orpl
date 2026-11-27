package com.groma.base.common;

import java.util.List;

/**
 * 通用分页响应体
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int pageNum,
        int pageSize
) {}
