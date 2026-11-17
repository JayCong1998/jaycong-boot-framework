package io.github.jaycong.order.domain.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.jaycong.order.domain.entity.Order;
import io.github.jaycong.order.infrastructure.client.GoodsClient;
import io.github.jaycong.order.infrastructure.mapper.OrderMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** 下单管理服务直接协调商品读取与持久化，订单规则由实体维护。 */
@Service
public class OrderManageService {
    private final GoodsClient goods;
    private final OrderMapper mapper;

    public OrderManageService(GoodsClient goods, OrderMapper mapper) {
        this.goods = goods;
        this.mapper = mapper;
    }

    /** 每次调用创建独立订单；远程读取完成后执行单条原子插入。 */
    public Order create(long userId, long goodsId, String remark) {
        var snapshot = goods.getPurchasableGoods(goodsId);
        var order = Order.create(IdWorker.getId(), userId, snapshot, remark, LocalDateTime.now());
        if (mapper.insert(order) != 1) {
            throw new IllegalStateException("订单保存失败");
        }
        return order;
    }
}
