package io.github.jaycong.order.controller;

import io.github.jaycong.satoken.context.AuthContext;
import io.github.jaycong.core.model.Result;
import io.github.jaycong.api.order.response.OrderResponse;
import io.github.jaycong.order.domain.entity.convertor.OrderConvertor;
import io.github.jaycong.order.domain.service.OrderManageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final AuthContext authContext;
    private final OrderManageService orders;

    public OrderController(AuthContext authContext, OrderManageService orders) {
        this.authContext = authContext;
        this.orders = orders;
    }

    @PostMapping
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var order = orders.create(authContext.requireUserId(), request.goodsId(), request.remark());
        return Result.success(OrderConvertor.toResponse(order));
    }
}
