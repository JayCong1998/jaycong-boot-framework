package io.github.jaycong.admin.request;

import jakarta.validation.constraints.*;

public record MemberStatusRequest(
        @NotNull @Min(0) @Max(1) Integer status,
        @NotNull @PositiveOrZero Integer lockVersion) {
    public io.github.jaycong.api.user.management.request.MemberStatusRequest toRpcRequest() {
        return new io.github.jaycong.api.user.management.request.MemberStatusRequest(status, lockVersion);
    }
}
