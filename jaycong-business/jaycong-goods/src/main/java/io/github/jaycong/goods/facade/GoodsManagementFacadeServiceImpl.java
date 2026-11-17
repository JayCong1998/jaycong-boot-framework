package io.github.jaycong.goods.facade;

import io.github.jaycong.api.goods.management.request.GoodsSaveRequest;
import io.github.jaycong.api.goods.management.request.GoodsStatusRequest;
import io.github.jaycong.api.goods.management.request.GoodsStockRequest;
import io.github.jaycong.api.goods.management.response.GoodsCategoryView;
import io.github.jaycong.api.goods.management.response.ManagedGoodsView;
import io.github.jaycong.core.model.PageResult;
import java.util.List;
import io.github.jaycong.api.goods.management.facade.GoodsManagementFacade;
import io.github.jaycong.goods.domain.service.GoodsManageService;
import org.springframework.web.bind.annotation.RestController;
import io.github.jaycong.goods.domain.entity.convertor.GoodsConvertor;

/** 实现公共契约并转换输出，管理行为交给领域服务。 */
@RestController
public class GoodsManagementFacadeServiceImpl implements GoodsManagementFacade {
    private final GoodsManageService service;
    public GoodsManagementFacadeServiceImpl(GoodsManageService service) {
        this.service = service;
    }

    @Override
    public PageResult<ManagedGoodsView> list(
            int page,
            int size,
            String name,
            String goodsCode,
            String category,
            Integer status,
            boolean lowStock
    ) {
        var result = service.list(page, size, name, goodsCode, category, status, lowStock);
        return new PageResult<>(result.total(), result.rows().stream().map(GoodsConvertor::toManagedView).toList());
    }

    @Override
    public ManagedGoodsView detail(long id) {
        return GoodsConvertor.toManagedView(service.detail(id));
    }

    @Override
    public ManagedGoodsView create(GoodsSaveRequest request) {
        return GoodsConvertor.toManagedView(service.create(request));
    }

    @Override
    public ManagedGoodsView update(
            long id,
            GoodsSaveRequest request
    ) {
        return GoodsConvertor.toManagedView(service.update(id, request));
    }

    @Override
    public ManagedGoodsView status(
            long id,
            GoodsStatusRequest request
    ) {
        return GoodsConvertor.toManagedView(service.status(id, request.status(), request.lockVersion()));
    }

    @Override
    public ManagedGoodsView stock(
            long id,
            GoodsStockRequest request
    ) {
        return GoodsConvertor.toManagedView(service.stock(id, request.quantity(), request.lockVersion()));
    }

    @Override
    public void delete(long id, int lockVersion) {
        service.delete(id, lockVersion);
    }

    @Override
    public List<GoodsCategoryView> categories() {
        return service.categories().stream().map(GoodsConvertor::toCategoryView).toList();
    }
}
