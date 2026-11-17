package io.github.jaycong.order.domain.entity;

import io.github.jaycong.order.OrderException;
import io.github.jaycong.order.OrderException.Reason;

/** 订单持有的商品值对象，成交金额单位为人民币分。 */
public record GoodsSnapshot(long goodsId, String goodsCode, String goodsName,
        String goodsCoverUrl, long priceInCents) {
    public GoodsSnapshot {
        if (goodsId <= 0 || goodsCode == null || goodsCode.isBlank() || goodsCode.length() > 64
                || goodsName == null || goodsName.isBlank() || goodsName.length() > 128
                || priceInCents <= 0) {
            throw new OrderException(Reason.INVALID_ORDER, "商品快照或成交金额无效");
        }
        goodsCoverUrl = goodsCoverUrl == null ? "" : goodsCoverUrl;
        if (goodsCoverUrl.length() > 512) {
            throw new OrderException(Reason.INVALID_ORDER, "商品封面地址过长");
        }
    }
}
