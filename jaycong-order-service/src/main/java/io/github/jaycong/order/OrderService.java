package io.github.jaycong.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.jaycong.core.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderMapper mapper;
    private final StockClient stockClient;
    public OrderService(OrderMapper mapper, StockClient stockClient) {
        this.mapper = mapper;
        this.stockClient = stockClient;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        var order = new Order();
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        mapper.insert(order);
        stockClient.deduct(request.productId(), request.quantity());
        // 此时 B 的事务已经提交；这里只会回滚 A 的订单。
        if (request.failAfterStockDeduction()) throw new BusinessException(OrderErrorCode.DEMO_FAILURE);
        return order;
    }

    public Order get(long id) {
        var order = mapper.selectById(id);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return order;
    }

    public List<Order> list() {
        return mapper.selectList(new LambdaQueryWrapper<Order>().orderByAsc(Order::getId));
    }
}
