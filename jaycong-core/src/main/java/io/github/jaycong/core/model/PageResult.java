package io.github.jaycong.core.model;

import java.util.List;

/** 分页结果。列表为不可修改的快照，列表元素本身不做深拷贝。 */
public record PageResult<T>(long total, List<T> rows) {
    public PageResult {
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        rows = List.copyOf(rows);
        if (rows.size() > total) {
            throw new IllegalArgumentException("row count must not exceed total");
        }
    }
}
