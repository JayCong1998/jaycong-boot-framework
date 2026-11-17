package io.github.jaycong.admin.request;

import jakarta.validation.constraints.*;

public record GoodsStockRequest(@NotNull Integer quantity, @NotNull @Min(0) Integer lockVersion) {
    public io.github.jaycong.api.goods.management.request.GoodsStockRequest toRpcRequest() {
        return new io.github.jaycong.api.goods.management.request.GoodsStockRequest(quantity, lockVersion);
    }
}
