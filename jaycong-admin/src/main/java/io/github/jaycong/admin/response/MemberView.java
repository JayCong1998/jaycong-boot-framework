package io.github.jaycong.admin.response;

import java.time.LocalDateTime;

/** Admin HTTP 响应，与远程协议分别维护。 */
public record MemberView(
        String id,
        String nickname,
        String mobile,
        String email,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime lastLoginTime,
        Integer lockVersion) {
    public static MemberView from(io.github.jaycong.api.user.management.response.MemberView value) {
        return new MemberView(
                value.id(),
                value.nickname(),
                value.mobile(),
                value.email(),
                value.status(),
                value.createTime(),
                value.updateTime(),
                value.lastLoginTime(),
                value.lockVersion());
    }
}
