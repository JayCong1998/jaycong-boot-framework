package io.github.jaycong.goods.domain.entity.convertor;

import io.github.jaycong.api.goods.management.request.GoodsSaveRequest;
import io.github.jaycong.api.goods.management.response.GoodsCategoryView;
import io.github.jaycong.api.goods.management.response.ManagedGoodsView;
import io.github.jaycong.api.goods.model.GoodsView;
import io.github.jaycong.goods.GoodsException;
import io.github.jaycong.goods.domain.entity.Goods;
import io.github.jaycong.goods.domain.entity.GoodsCategory;
import io.github.jaycong.goods.domain.entity.GoodsDetails;
import java.math.BigDecimal;

/** 集中处理商品资料及对外展示映射，不执行查询或业务状态迁移。 */
public final class GoodsConvertor {
    private GoodsConvertor() { }

    public static GoodsDetails toDetails(GoodsSaveRequest request) {
        return new GoodsDetails(request.name(), request.goodsCode(), request.subtitle(), request.category(),
                request.coverUrl(), request.description(), cents(request.price()),
                request.originalPrice() == null ? null : cents(request.originalPrice()),
                request.stockWarning(), request.sortOrder());
    }

    public static GoodsView toView(Goods goods) {
        return new GoodsView(goods.getId().toString(), goods.getGoodsCode(), goods.getName(), goods.getSubtitle(),
                BigDecimal.valueOf(goods.getPrice(), 2), goods.getCoverUrl(), goods.getDescription());
    }

    public static ManagedGoodsView toManagedView(Goods goods) {
        var details = goods.details().normalized();
        return new ManagedGoodsView(goods.getId().toString(), details.goodsCode(), details.name(), details.subtitle(),
                details.category(), details.coverUrl(), details.description(), BigDecimal.valueOf(details.price(), 2),
                details.originalPrice() == null ? null : BigDecimal.valueOf(details.originalPrice(), 2), goods.getStock(),
                details.stockWarning(), details.sortOrder(), goods.getStatus(), goods.getLockVersion(),
                goods.getCreateTime(), goods.getUpdateTime());
    }

    public static GoodsCategoryView toCategoryView(GoodsCategory category) {
        return new GoodsCategoryView(category.value(), category.value());
    }

    private static long cents(BigDecimal value) {
        if (value == null) {
            throw new GoodsException(400, "请提交商品价格");
        }
        try {
            return value.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new GoodsException(400, "价格最多保留两位小数");
        }
    }
}
