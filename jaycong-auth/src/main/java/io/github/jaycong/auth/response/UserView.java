package io.github.jaycong.auth.response;

import io.github.jaycong.api.user.response.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/** 认证接口用户信息，不暴露远程协议和用户持久化模型。 */
@Schema(description = "认证用户信息")
public record UserView(
        @Schema(description = "用户 ID，字符串格式避免前端精度丢失", example = "1") String id,
        @Schema(description = "手机号，未填写时为空") String mobile,
        @Schema(description = "邮箱，未填写时为空") String email,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "用户角色", allowableValues = {"admin", "user"}) String role) {
    public static UserView from(UserInfo user) {
        return new UserView(user.id(), user.mobile(), user.email(), user.nickname(), user.role());
    }
}
