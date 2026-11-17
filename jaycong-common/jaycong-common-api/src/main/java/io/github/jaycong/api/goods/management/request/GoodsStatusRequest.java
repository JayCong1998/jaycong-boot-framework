package io.github.jaycong.api.goods.management.request;

import jakarta.validation.constraints.*;

public record GoodsStatusRequest(@NotNull @Min(0) @Max(1) Integer status, @NotNull @Min(0) Integer lockVersion) {}
