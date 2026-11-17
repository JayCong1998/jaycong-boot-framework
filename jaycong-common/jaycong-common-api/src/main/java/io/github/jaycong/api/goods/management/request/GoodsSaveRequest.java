package io.github.jaycong.api.goods.management.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GoodsSaveRequest(@NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 64) String goodsCode, @Size(max = 255) String subtitle,
        @NotBlank @Size(max = 64) String category, @Size(max = 512) String coverUrl,
        @Size(max = 20000) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer = 9, fraction = 2) BigDecimal price,
        @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer = 9, fraction = 2) BigDecimal originalPrice,
        @NotNull @Min(0) Integer stockWarning, @NotNull @Min(0) Integer sortOrder, @Min(0) Integer lockVersion) {}
