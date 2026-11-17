package io.github.jaycong.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** tokenValue 放入 Authorization: Bearer 请求头，timeout 单位为秒。 */
@Schema(description = "登录结果")
public record LoginResponse(
        @Schema(description = "Token 请求头名称", example = "Authorization") String tokenName,
        @Schema(description = "Token 值，填入 Swagger UI 的 Authorize；实际请求由 UI 添加 Bearer 前缀") String tokenValue,
        @Schema(description = "Token 剩余有效期，单位秒", example = "86400") long timeout,
        @Schema(description = "当前登录用户") UserView user) {
}
