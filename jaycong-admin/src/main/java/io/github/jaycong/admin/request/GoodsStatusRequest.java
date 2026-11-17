package io.github.jaycong.admin.request;

import jakarta.validation.constraints.*;

public record GoodsStatusRequest(@NotNull @Min(0) @Max(1) Integer status, @NotNull @Min(0) Integer lockVersion) {
    public io.github.jaycong.api.goods.management.request.GoodsStatusRequest toRpcRequest() {
        return new io.github.jaycong.api.goods.management.request.GoodsStatusRequest(status, lockVersion);
    }
}
