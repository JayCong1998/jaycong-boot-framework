package io.github.jaycong.api.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户服务的远程请求；日志输出隐藏凭证及联系方式。 */
public record UserRegisterRequest(@Size(max = 32) String mobile, @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 64) String password,
                                  @NotBlank @Size(max = 64) String nickname) {
    @Override
    public String toString() { return "UserRegisterRequest[redacted]"; }
}
