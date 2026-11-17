package io.github.jaycong.order.domain.entity.convertor;

import io.github.jaycong.api.order.response.OrderResponse;
import io.github.jaycong.order.domain.entity.Order;

/** 对外响应只包含订单展示字段，避免泄露实体的审计及持久化细节。 */
public final class OrderConvertor {
    private OrderConvertor() { }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId().toString(), order.getOrderNo(),
                order.getGoodsId().toString(), order.getGoodsCode(), order.getGoodsName(), order.getGoodsCoverUrl(),
                order.getTotalAmount(), order.getStatus(), order.getRemark(), order.getExpireTime());
    }
}
