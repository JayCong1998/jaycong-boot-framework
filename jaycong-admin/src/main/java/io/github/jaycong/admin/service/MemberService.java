package io.github.jaycong.admin.service;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import io.github.jaycong.api.user.management.client.MemberManagementClient;
import org.springframework.stereotype.Service;

/** 管理用例远程编排；事务与业务校验由所属服务负责。 */
@Service
public class MemberService {
    private final AdminRemoteService remote;
    private final MemberManagementClient client;
    public MemberService(MemberManagementClient client, AdminRemoteService remote) {
        this.client = client;
        this.remote = remote;
    }

    public PageResult<MemberView> list(
            int page,
            int size,
            String keyword,
            Long id,
            Integer status,
            LocalDateTime createTimeStart,
            LocalDateTime createTimeEnd
    ) {
        var result = remote.call(() -> client.list(page, size, keyword, id, status, createTimeStart, createTimeEnd));
        return new PageResult<>(result.total(), result.rows().stream().map(MemberView::from).toList());
    }

    public MemberView detail(long id) {
        return MemberView.from(remote.call(() -> client.detail(id)));
    }

    public MemberView update(long id, MemberUpdateRequest request) {
        return MemberView.from(remote.call(() -> client.update(id, request.toRpcRequest())));
    }

    public MemberView status(long id, MemberStatusRequest request) {
        return MemberView.from(remote.call(() -> client.status(id, request.toRpcRequest())));
    }
}
