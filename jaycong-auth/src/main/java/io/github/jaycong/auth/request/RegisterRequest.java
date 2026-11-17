package io.github.jaycong.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/** 手机号、邮箱至少提供一项，空白标识会转为 NULL。 */
@Schema(description = "用户注册请求，手机号和邮箱至少提供一项")
public record RegisterRequest(
        @Schema(description = "手机号；与邮箱至少填写一项", example = "13800138002")
        @Size(max = 32) String mobile,
        @Schema(description = "邮箱；与手机号至少填写一项", example = "member@example.com")
        @Size(max = 254) String email,
        @Schema(description = "密码，8 至 64 个字符", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 64) String password,
        @Schema(description = "昵称", example = "示例用户")
        @NotBlank @Size(max = 64) String nickname) {

    @Override
    public String toString() { return "RegisterRequest[redacted]"; }
}
