package io.github.jaycong.admin.request;

import jakarta.validation.constraints.*;

public record MemberUpdateRequest(
        @NotBlank @Size(max = 64) String nickname,
        @NotNull @PositiveOrZero Integer lockVersion) {
    public io.github.jaycong.api.user.management.request.MemberUpdateRequest toRpcRequest() {
        return new io.github.jaycong.api.user.management.request.MemberUpdateRequest(nickname, lockVersion);
    }
}
