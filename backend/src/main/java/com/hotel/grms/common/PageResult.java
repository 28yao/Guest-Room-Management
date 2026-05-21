package com.hotel.grms.common;

import java.util.List;

/**
 * 分页查询结果包装。
 *
 * @param <T> 记录类型
 * @author liuxinsi
 * @date 2026-05-21
 */
public class PageResult<T> {

    private long total;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
