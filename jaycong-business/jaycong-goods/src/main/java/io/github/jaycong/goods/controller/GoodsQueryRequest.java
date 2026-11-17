package io.github.jaycong.goods.controller;

import io.github.jaycong.core.model.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 页码从 1 开始；名称为包含匹配，空白筛选条件忽略。 */
public class GoodsQueryRequest {
    @Min(1)
    private int page = 1;
    @Min(1)
    @Max(PageRequest.MAX_SIZE)
    private int size = PageRequest.DEFAULT_SIZE;
    @Size(max = 128)
    private String name;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

