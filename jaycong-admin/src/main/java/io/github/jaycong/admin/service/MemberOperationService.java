package io.github.jaycong.admin.service;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import io.github.jaycong.api.user.management.client.MemberOperationManagementClient;
import org.springframework.stereotype.Service;

/** 管理用例远程编排；事务与业务校验由所属服务负责。 */
@Service
public class MemberOperationService {
    private final AdminRemoteService remote;
    private final MemberOperationManagementClient client;
    public MemberOperationService(MemberOperationManagementClient client, AdminRemoteService remote) {
        this.client = client;
        this.remote = remote;
    }

    public PageResult<MemberOperationView> list(
            int page,
            int size,
            Long memberId,
            Long operatorId,
            String action,
            String operatorType,
            String outcome,
            LocalDateTime start,
            LocalDateTime end
    ) {
        var result = remote.call(() -> client.list(page, size, memberId, operatorId, action, operatorType, outcome, start, end));
        return new PageResult<>(result.total(), result.rows().stream().map(MemberOperationView::from).toList());
    }

    public MemberOperationView detail(long id) {
        return MemberOperationView.from(remote.call(() -> client.detail(id)));
    }
}
