package io.github.jaycong.api.user.response;

/** 用户基本信息，ID 使用字符串避免调用方丢失 BIGINT 精度；不包含密码哈希。 */
public record UserInfo(String id, String mobile, String email, String nickname, String role) {
}
