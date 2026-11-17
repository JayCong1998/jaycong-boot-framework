package io.github.jaycong.goods.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.jaycong.api.goods.management.request.GoodsSaveRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.goods.GoodsException;
import io.github.jaycong.goods.domain.entity.Goods;
import io.github.jaycong.goods.domain.entity.GoodsCategory;
import io.github.jaycong.goods.infrastructure.mapper.GoodsMapper;
import io.github.jaycong.satoken.context.AuthContext;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 负责商品授权、事务和持久化，实体维护业务规则。 */
@Service
public class GoodsManageService {
    private final GoodsMapper mapper;
    private final GoodsReadService reads;
    private final AuthContext authContext;

    public GoodsManageService(GoodsMapper mapper, GoodsReadService reads, AuthContext authContext) {
        this.mapper = mapper;
        this.reads = reads;
        this.authContext = authContext;
    }

    public PageResult<Goods> list(int page, int size, String name, String goodsCode,
            String category, Integer status, boolean lowStock) {
        authContext.requireAdmin();
        return reads.pageManaged(page, size, name, goodsCode, category, status, lowStock);
    }

    public Goods detail(long id) {
        authContext.requireAdmin();
        return reads.getGoods(id);
    }

    @Transactional
    public Goods create(GoodsSaveRequest request) {
        authContext.requireAdmin();
        var goods = Goods.create(request);
        try {
            mapper.insert(goods);
        } catch (DuplicateKeyException exception) {
            throw duplicateGoodsCode(exception);
        }
        return reads.getGoods(goods.getId());
    }

    @Transactional
    public Goods update(long id, GoodsSaveRequest request) {
        authContext.requireAdmin();
        if (request.lockVersion() == null) {
            throw new GoodsException(400, "请提交lockVersion版本号");
        }
        var goods = reads.getGoods(id);
        goods.requireVersion(request.lockVersion());
        goods.changeDetails(request);
        saveChanges(goods);
        return reads.getGoods(id);
    }

    @Transactional
    public Goods status(long id, int status, int version) {
        authContext.requireAdmin();
        var goods = reads.getGoods(id);
        goods.requireVersion(version);
        goods.changeStatus(status);
        saveChanges(goods);
        return reads.getGoods(id);
    }

    @Transactional
    public Goods stock(long id, int quantity, int version) {
        authContext.requireAdmin();
        var goods = reads.getGoods(id);
        goods.requireVersion(version);
        goods.adjustStock(quantity);
        saveChanges(goods);
        return reads.getGoods(id);
    }

    @Transactional
    public void delete(long id, int version) {
        authContext.requireAdmin();
        var goods = reads.getGoods(id);
        goods.requireVersion(version);
        goods.requireDeletable();
        // 同一更新维护审计、乐观锁和逻辑删除；历史订单保留自己的成交快照。
        int updated = mapper.update(goods, Wrappers.<Goods>lambdaUpdate()
                .eq(Goods::getId, id).eq(Goods::getLockVersion, version)
                .eq(Goods::getStatus, 0).set(Goods::getDeleted, 1));
        if (updated != 1) {
            throw conflict();
        }
    }

    public List<GoodsCategory> categories() {
        authContext.requireAdmin();
        return List.of(GoodsCategory.values());
    }

    private void saveChanges(Goods goods) {
        try {
            if (mapper.updateById(goods) != 1) {
                throw conflict();
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateGoodsCode(exception);
        }
    }

    private GoodsException duplicateGoodsCode(DuplicateKeyException exception) {
        return new GoodsException(409, "商品编码已存在（含已删除商品），请使用其他编码", exception);
    }

    private GoodsException conflict() {
        return new GoodsException(409, "数据已发生变化，请刷新后重试");
    }
}
