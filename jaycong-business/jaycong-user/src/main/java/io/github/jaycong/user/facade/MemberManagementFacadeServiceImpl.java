package io.github.jaycong.user.facade;

import io.github.jaycong.api.user.management.facade.MemberManagementFacade;
import io.github.jaycong.api.user.management.request.MemberStatusRequest;
import io.github.jaycong.api.user.management.request.MemberUpdateRequest;
import io.github.jaycong.api.user.management.response.MemberView;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.user.domain.entity.convertor.UserConvertor;
import io.github.jaycong.user.domain.service.UserManageService;
import io.github.jaycong.user.domain.service.UserReadService;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.RestController;

/** 内部会员管理入口，复用公共请求契约并转换领域结果。 */
@RestController
public class MemberManagementFacadeServiceImpl implements MemberManagementFacade {
    private final UserReadService reads;
    private final UserManageService manages;

    public MemberManagementFacadeServiceImpl(UserReadService reads, UserManageService manages) {
        this.reads = reads;
        this.manages = manages;
    }

    @Override
    public PageResult<MemberView> list(int page, int size, String keyword, Long id, Integer status,
            LocalDateTime createTimeStart, LocalDateTime createTimeEnd) {
        var result = reads.listMembers(page, size, keyword, id, status, createTimeStart, createTimeEnd);
        return new PageResult<>(result.total(), result.rows().stream().map(UserConvertor::toMemberView).toList());
    }

    @Override
    public MemberView detail(long id) {
        return UserConvertor.toMemberView(reads.memberDetail(id));
    }

    @Override
    public MemberView update(long id, MemberUpdateRequest request) {
        return UserConvertor.toMemberView(manages.updateMember(id, request));
    }

    @Override
    public MemberView status(long id, MemberStatusRequest request) {
        return UserConvertor.toMemberView(manages.changeMemberStatus(id, request));
    }
}
