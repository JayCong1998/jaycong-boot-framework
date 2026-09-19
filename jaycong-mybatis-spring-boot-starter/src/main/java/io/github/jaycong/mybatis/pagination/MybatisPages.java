package io.github.jaycong.mybatis.pagination;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import java.util.Objects;

/** 将数据库分页类型限制在持久层，API 继续使用 Core 分页契约。 */
public final class MybatisPages {
    private MybatisPages() { }

    public static <T> Page<T> toPage(PageRequest request) {
        Objects.requireNonNull(request, "request");
        return new Page<>(request.page(), request.size());
    }

    public static <T> PageResult<T> toResult(IPage<T> page) {
        Objects.requireNonNull(page, "page");
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
