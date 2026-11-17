package io.github.jaycong.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.jaycong.core.model.BaseEntity;
import io.github.jaycong.user.UserException;
import lombok.Getter;

/** 只记录允许的变更摘要及实际完成阶段，不保存请求体或认证凭证。 */
@Getter
@TableName("member_operation_log")
public class MemberOperation extends BaseEntity {
    private Long memberId;
    private Long operatorId;
    private String operatorName;
    private String operatorType;
    private String source;
    private String action;
    private String outcome;
    private String beforeValue;
    private String afterValue;
    private String message;

    /** 操作人必须来自服务端核验后的用户，不接收客户端指定的身份或来源。 */
    public static MemberOperation start(User actor, long memberId, String action) {
        var operation = new MemberOperation();
        operation.memberId = memberId;
        operation.operatorId = actor.getId();
        operation.operatorName = actor.getNickname();
        boolean admin = "admin".equals(actor.getRole());
        operation.operatorType = admin ? "ADMIN" : "MEMBER";
        operation.source = admin ? "ADMIN_WEB" : "MEMBER_WEB";
        operation.action = action;
        operation.outcome = "PROCESSING";
        operation.message = "操作已接收，结果待确认";
        operation.setCreateUserId(actor.getId());
        operation.setUpdateUserId(actor.getId());
        return operation;
    }

    public void recordChanges(String before, String after) {
        beforeValue = before;
        afterValue = after;
    }

    public void databaseCompleted(boolean sessionsPending) {
        outcome = sessionsPending ? "PARTIAL" : "SUCCESS";
        message = sessionsPending ? "数据库操作已提交，会话撤销待确认" : "操作成功";
    }

    public void complete() {
        outcome = "SUCCESS";
        message = "操作成功";
    }

    /** 以持久化阶段为准，不能把提交成功但后续失败的操作标记成数据库失败。 */
    public void recordFailure(RuntimeException exception, boolean databaseStarted) {
        if ("SUCCESS".equals(outcome)) {
            return;
        }
        if ("PARTIAL".equals(outcome)) {
            message = "数据库操作已提交，会话撤销或结果记录未完成，请重试撤销并核实";
        } else if (!databaseStarted || exception instanceof UserException) {
            outcome = "FAILED";
            message = failureMessage(exception);
        } else {
            outcome = "PROCESSING";
            message = "操作结果待核实，请刷新会员资料及记录；旧会话可能已撤销";
        }
    }

    private String failureMessage(RuntimeException exception) {
        if (exception instanceof UserException business) {
            return switch (business.getStatus()) {
                case 400 -> "参数校验失败，数据库未变更";
                case 404 -> "会员不存在，数据库未变更";
                case 409 -> "会员版本冲突，数据库未变更";
                default -> "操作未完成，请刷新资料后核实";
            };
        }
        return "操作未完成，请刷新资料后核实；启用前可能已撤销旧会话";
    }
}
