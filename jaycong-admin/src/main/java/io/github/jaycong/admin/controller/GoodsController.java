package io.github.jaycong.admin.controller;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.github.jaycong.admin.service.GoodsService;
import io.github.jaycong.core.model.Result;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "商品管理", description = "统一由 Admin 校验管理员身份并远程调用业务服务")
@RestController
@SecurityRequirement(name = "bearerAuth")
public class GoodsController {
    private final GoodsService service;
    public GoodsController(GoodsService service) {
        this.service = service;
    }

    @GetMapping("/admin/goods")
    @Operation(summary = "分页查询商品管理")
    public Result<PageResult<ManagedGoodsView>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "goodsCode", required = false) String goodsCode,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "lowStock", defaultValue = "false") boolean lowStock
    ) {
        return Result.success(service.list(page, size, name, goodsCode, category, status, lowStock));
    }

    @GetMapping("/admin/goods/{id}")
    @Operation(summary = "查询商品管理详情")
    public Result<ManagedGoodsView> detail(@PathVariable("id") long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/admin/goods")
    @Operation(summary = "创建下架商品")
    public Result<ManagedGoodsView> create(
            @Valid @RequestBody GoodsSaveRequest request
    ) {
        return Result.success(service.create(request));
    }

    @PutMapping("/admin/goods/{id}")
    @Operation(summary = "修改资料（提交 lockVersion）")
    public Result<ManagedGoodsView> update(
            @PathVariable("id") long id,
            @Valid @RequestBody GoodsSaveRequest request
    ) {
        return Result.success(service.update(id, request));
    }

    @PutMapping("/admin/goods/{id}/status")
    @Operation(summary = "修改状态（提交 lockVersion）")
    public Result<ManagedGoodsView> status(
            @PathVariable("id") long id,
            @Valid @RequestBody GoodsStatusRequest request
    ) {
        return Result.success(service.status(id, request));
    }

    @PutMapping("/admin/goods/{id}/stock")
    @Operation(summary = "调整库存增量（提交 lockVersion）")
    public Result<ManagedGoodsView> stock(
            @PathVariable("id") long id,
            @Valid @RequestBody GoodsStockRequest request
    ) {
        return Result.success(service.stock(id, request));
    }

    @DeleteMapping("/admin/goods/{id}")
    @Operation(summary = "逻辑删除下架商品（提交 lockVersion）")
    public Result<Void> delete(
            @PathVariable("id") long id,
            @RequestParam("lockVersion") int lockVersion
    ) {
        service.delete(id, lockVersion);
        return Result.success();
    }

    @GetMapping("/admin/goods-categories")
    @Operation(summary = "查询固定商品分类")
    public Result<List<GoodsCategoryView>> categories() {
        return Result.success(service.categories());
    }
}
