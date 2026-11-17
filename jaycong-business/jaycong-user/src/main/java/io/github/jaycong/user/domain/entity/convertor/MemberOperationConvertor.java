package io.github.jaycong.user.domain.entity.convertor;

import io.github.jaycong.user.domain.entity.MemberOperation;
import io.github.jaycong.api.user.management.response.MemberOperationView;

/** 对外只输出可查询的操作摘要，不返回持久化控制字段。 */
public final class MemberOperationConvertor {
    private MemberOperationConvertor() { }
    public static MemberOperationView toView(MemberOperation o) {
        return new MemberOperationView(o.getId().toString(), o.getMemberId().toString(),
                o.getOperatorId().toString(), o.getOperatorName(), o.getOperatorType(), o.getSource(),
                o.getAction(), o.getOutcome(), o.getBeforeValue(), o.getAfterValue(), o.getMessage(),
                o.getCreateTime(), o.getUpdateTime());
    }
}
