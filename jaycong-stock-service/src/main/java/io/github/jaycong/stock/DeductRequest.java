package io.github.jaycong.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeductRequest(@NotNull @Positive Long productId, @NotNull @Positive Integer quantity) {}
