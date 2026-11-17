package io.github.jaycong.order.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 一次购买一个商品；用户、价格、商品快照均由服务端确定。 */
public record CreateOrderRequest(@NotNull @Positive Long goodsId, @Size(max = 500) String remark) {}
