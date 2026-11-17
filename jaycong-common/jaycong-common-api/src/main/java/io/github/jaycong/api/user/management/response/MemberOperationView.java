package io.github.jaycong.api.user.management.response;

import java.time.LocalDateTime;

/** 管理 RPC 与 Admin 共用的响应字段，不包含持久化模型。 */
public record MemberOperationView(String id, String memberId, String operatorId,
        String operatorName, String operatorType, String source, String action, String outcome,
        String beforeValue, String afterValue, String message, LocalDateTime createTime, LocalDateTime updateTime) {}
