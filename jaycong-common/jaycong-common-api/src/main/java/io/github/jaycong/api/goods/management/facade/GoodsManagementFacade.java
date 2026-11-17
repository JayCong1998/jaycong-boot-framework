package io.github.jaycong.api.goods.management.facade;

import io.github.jaycong.api.goods.management.request.*;
import io.github.jaycong.api.goods.management.response.*;
import io.github.jaycong.core.model.PageResult;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 仅供 Admin 调用的内部管理契约；服务凭证与登录会话均须校验。 */
public interface GoodsManagementFacade {
    @GetMapping("/internal/admin/goods")
    PageResult<ManagedGoodsView> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "goodsCode", required = false) String goodsCode,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "lowStock", defaultValue = "false") boolean lowStock
    );

    @GetMapping("/internal/admin/goods/{id}")
    ManagedGoodsView detail(@PathVariable("id") long id);

    @PostMapping("/internal/admin/goods")
    ManagedGoodsView create(@Valid @RequestBody GoodsSaveRequest request);

    @PutMapping("/internal/admin/goods/{id}")
    ManagedGoodsView update(@PathVariable("id") long id, @Valid @RequestBody GoodsSaveRequest request);

    @PutMapping("/internal/admin/goods/{id}/status")
    ManagedGoodsView status(@PathVariable("id") long id, @Valid @RequestBody GoodsStatusRequest request);

    @PutMapping("/internal/admin/goods/{id}/stock")
    ManagedGoodsView stock(@PathVariable("id") long id, @Valid @RequestBody GoodsStockRequest request);

    @DeleteMapping("/internal/admin/goods/{id}")
    void delete(@PathVariable("id") long id, @RequestParam("lockVersion") int lockVersion);

    @GetMapping("/internal/admin/goods-categories")
    List<GoodsCategoryView> categories();

}
