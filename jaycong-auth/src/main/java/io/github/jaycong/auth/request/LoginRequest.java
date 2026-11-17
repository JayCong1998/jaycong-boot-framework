package io.github.jaycong.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "手机号或邮箱密码登录请求")
public record LoginRequest(
        @Schema(description = "手机号或邮箱，邮箱登录不区分大小写", example = "13800138000")
        @NotBlank @Size(max = 254) String account,
        @Schema(description = "密码，8 至 64 个字符", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 64) String password) {

    @Override
    public String toString() { return "LoginRequest[redacted]"; }
}
