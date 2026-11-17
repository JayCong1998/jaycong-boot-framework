package io.github.jaycong.api.user.management.request;

import jakarta.validation.constraints.*;

public record MemberUpdateRequest(
        @NotBlank @Size(max = 64) String nickname,
        @NotNull @PositiveOrZero Integer lockVersion) {}
