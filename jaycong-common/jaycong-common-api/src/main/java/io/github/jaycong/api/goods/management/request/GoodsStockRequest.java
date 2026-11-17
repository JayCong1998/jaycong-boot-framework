package io.github.jaycong.api.goods.management.request;

import jakarta.validation.constraints.*;

public record GoodsStockRequest(@NotNull Integer quantity, @NotNull @Min(0) Integer lockVersion) {}
