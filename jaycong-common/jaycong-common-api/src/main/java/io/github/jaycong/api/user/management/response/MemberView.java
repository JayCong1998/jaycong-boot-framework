package io.github.jaycong.api.user.management.response;

import java.time.LocalDateTime;

/** 管理 RPC 与 Admin 共用的响应字段，不包含持久化模型。 */
public record MemberView(
        String id,
        String nickname,
        String mobile,
        String email,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime lastLoginTime,
        Integer lockVersion) {}
