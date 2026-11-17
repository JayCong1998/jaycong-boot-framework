package io.github.jaycong.order.infrastructure.client;

import feign.FeignException;
import io.github.jaycong.api.goods.client.GoodsFeignClient;
import io.github.jaycong.api.goods.model.GoodsView;
import io.github.jaycong.order.OrderException;
import io.github.jaycong.order.OrderException.Reason;
import io.github.jaycong.order.domain.entity.GoodsSnapshot;
import org.springframework.stereotype.Component;

/** 商品服务的防腐适配：将远程 DTO 和元价格转换为订单自己的快照和整数分。 */
@Component
public class GoodsClient {
    private final GoodsFeignClient client;

    public GoodsClient(GoodsFeignClient client) { this.client = client; }

    public GoodsSnapshot getPurchasableGoods(long goodsId) {
        try {
            var goods = client.getAvailable(goodsId);
            if (goods == null) {
                throw new OrderException(Reason.GOODS_UNAVAILABLE, "商品服务响应异常");
            }
            if (!Long.toString(goodsId).equals(goods.id()) || goods.price() == null) {
                throw new OrderException(Reason.GOODS_UNAVAILABLE, "商品数据异常");
            }
            return toSnapshot(goodsId, goods);
        } catch (FeignException.NotFound exception) {
            throw new OrderException(Reason.GOODS_NOT_PURCHASABLE, "商品不存在或未上架");
        } catch (FeignException | ArithmeticException exception) {
            throw new OrderException(Reason.GOODS_UNAVAILABLE, "商品服务暂不可用", exception);
        }
    }

    private GoodsSnapshot toSnapshot(long goodsId, GoodsView goods) {
        try {
            // 精确转换，不对外部金额进行四舍五入。
            return new GoodsSnapshot(goodsId, goods.goodsCode(), goods.name(), goods.image(),
                    goods.price().movePointRight(2).longValueExact());
        } catch (OrderException exception) {
            throw new OrderException(Reason.GOODS_UNAVAILABLE, "商品数据异常", exception);
        }
    }
}
