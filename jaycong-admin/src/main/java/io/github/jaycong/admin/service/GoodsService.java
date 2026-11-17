package io.github.jaycong.admin.service;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.util.List;
import io.github.jaycong.api.goods.management.client.GoodsManagementClient;
import org.springframework.stereotype.Service;

/** 管理用例远程编排；事务与业务校验由所属服务负责。 */
@Service
public class GoodsService {
    private final AdminRemoteService remote;
    private final GoodsManagementClient client;
    public GoodsService(GoodsManagementClient client, AdminRemoteService remote) {
        this.client = client;
        this.remote = remote;
    }

    public PageResult<ManagedGoodsView> list(
            int page,
            int size,
            String name,
            String goodsCode,
            String category,
            Integer status,
            boolean lowStock
    ) {
        var result = remote.call(() -> client.list(page, size, name, goodsCode, category, status, lowStock));
        return new PageResult<>(result.total(), result.rows().stream().map(ManagedGoodsView::from).toList());
    }

    public ManagedGoodsView detail(long id) {
        return ManagedGoodsView.from(remote.call(() -> client.detail(id)));
    }

    public ManagedGoodsView create(GoodsSaveRequest request) {
        return ManagedGoodsView.from(remote.call(() -> client.create(request.toRpcRequest())));
    }

    public ManagedGoodsView update(long id, GoodsSaveRequest request) {
        return ManagedGoodsView.from(remote.call(() -> client.update(id, request.toRpcRequest())));
    }

    public ManagedGoodsView status(long id, GoodsStatusRequest request) {
        return ManagedGoodsView.from(remote.call(() -> client.status(id, request.toRpcRequest())));
    }

    public ManagedGoodsView stock(long id, GoodsStockRequest request) {
        return ManagedGoodsView.from(remote.call(() -> client.stock(id, request.toRpcRequest())));
    }

    public void delete(long id, int lockVersion) {
        remote.run(() -> client.delete(id, lockVersion));
    }

    public List<GoodsCategoryView> categories() {
        return remote.call(client::categories).stream().map(GoodsCategoryView::from).toList();
    }
}
