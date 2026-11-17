package io.github.jaycong.admin.response;

import java.time.LocalDateTime;

/** Admin HTTP 响应，与远程协议分别维护。 */
public record MemberOperationView(String id, String memberId, String operatorId,
        String operatorName, String operatorType, String source, String action, String outcome,
        String beforeValue, String afterValue, String message, LocalDateTime createTime, LocalDateTime updateTime) {
    public static MemberOperationView from(io.github.jaycong.api.user.management.response.MemberOperationView value) {
        return new MemberOperationView(
                value.id(),
                value.memberId(),
                value.operatorId(),
                value.operatorName(),
                value.operatorType(),
                value.source(),
                value.action(),
                value.outcome(),
                value.beforeValue(),
                value.afterValue(),
                value.message(),
                value.createTime(),
                value.updateTime());
    }
}
