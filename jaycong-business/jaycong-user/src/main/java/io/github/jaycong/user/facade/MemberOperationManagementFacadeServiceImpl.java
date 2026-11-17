package io.github.jaycong.user.facade;

import io.github.jaycong.api.user.management.facade.MemberOperationManagementFacade;
import io.github.jaycong.api.user.management.response.MemberOperationView;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.user.domain.entity.convertor.MemberOperationConvertor;
import io.github.jaycong.user.domain.service.MemberOperationReadService;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.RestController;

/** 内部操作流水入口，查询及权限规则由读取服务处理。 */
@RestController
public class MemberOperationManagementFacadeServiceImpl implements MemberOperationManagementFacade {
    private final MemberOperationReadService reads;

    public MemberOperationManagementFacadeServiceImpl(MemberOperationReadService reads) {
        this.reads = reads;
    }

    @Override
    public PageResult<MemberOperationView> list(int page, int size, Long memberId, Long operatorId,
            String action, String operatorType, String outcome, LocalDateTime start, LocalDateTime end) {
        var result = reads.list(page, size, memberId, operatorId, action, operatorType, outcome, start, end);
        return new PageResult<>(result.total(), result.rows().stream().map(MemberOperationConvertor::toView).toList());
    }

    @Override
    public MemberOperationView detail(long id) {
        return MemberOperationConvertor.toView(reads.detail(id));
    }
}
