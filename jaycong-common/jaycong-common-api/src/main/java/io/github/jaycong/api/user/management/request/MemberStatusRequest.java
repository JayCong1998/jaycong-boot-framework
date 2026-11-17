package io.github.jaycong.api.user.management.request;

import jakarta.validation.constraints.*;

public record MemberStatusRequest(
        @NotNull @Min(0) @Max(1) Integer status,
        @NotNull @PositiveOrZero Integer lockVersion) {}
