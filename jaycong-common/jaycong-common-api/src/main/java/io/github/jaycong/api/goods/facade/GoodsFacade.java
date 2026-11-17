package io.github.jaycong.api.goods.facade;

import io.github.jaycong.api.goods.model.GoodsView;

/** 商品查询契约，调用方复用公开商品模型，不依赖商品服务的实体。 */
public interface GoodsFacade {
    /**
     * 查询未删除且已上架的商品，价格单位为元。
     * @param id 商品 ID
     * @return 公开商品资料
     * @throws RuntimeException 商品不存在或未上架时通过 HTTP 404 表达，其他调用故障由调用方转换
     */
    GoodsView getAvailable(long id);
}
