package io.github.jaycong.user.domain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.core.model.PageResult;
import io.github.jaycong.datasource.pagination.MybatisPages;
import io.github.jaycong.user.UserException;
import io.github.jaycong.user.domain.entity.MemberOperation;
import io.github.jaycong.user.infrastructure.mapper.MemberOperationMapper;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 管理员按固定筛选条件读取操作流水，不提供流水编辑或删除入口。 */
@Service
public class MemberOperationReadService {
    private final MemberOperationMapper mapper;
    private final UserReadService users;

    public MemberOperationReadService(MemberOperationMapper mapper, UserReadService users) {
        this.mapper = mapper;
        this.users = users;
    }

    public PageResult<MemberOperation> list(int page, int size, Long memberId, Long operatorId,
            String action, String operatorType, String outcome, LocalDateTime start, LocalDateTime end) {
        if (page < 1 || size < 1 || size > 100 || (memberId != null && memberId <= 0)
                || (operatorId != null && operatorId <= 0)
                || (start != null && end != null && start.isAfter(end))
                || (action != null && !Set.of("UPDATE_NICKNAME", "ENABLE", "DISABLE", "REVOKE_SESSIONS").contains(action))
                || (operatorType != null && !Set.of("ADMIN", "MEMBER").contains(operatorType))
                || (outcome != null && !Set.of("PROCESSING", "SUCCESS", "FAILED", "PARTIAL").contains(outcome))) {
            throw new UserException(400, "操作记录查询条件无效");
        }
        users.requireAdmin();
        var query = Wrappers.<MemberOperation>lambdaQuery()
                .eq(memberId != null, MemberOperation::getMemberId, memberId)
                .eq(operatorId != null, MemberOperation::getOperatorId, operatorId)
                .eq(action != null, MemberOperation::getAction, action)
                .eq(operatorType != null, MemberOperation::getOperatorType, operatorType)
                .eq(outcome != null, MemberOperation::getOutcome, outcome)
                .ge(start != null, MemberOperation::getCreateTime, start)
                .le(end != null, MemberOperation::getCreateTime, end)
                .orderByDesc(MemberOperation::getCreateTime, MemberOperation::getId);
        var result = mapper.selectPage(MybatisPages.toPage(new PageRequest(page, size)), query);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public MemberOperation detail(long id) {
        users.requireAdmin();
        if (id <= 0) {
            throw new UserException(400, "操作记录 ID 无效");
        }
        var operation = mapper.selectById(id);
        if (operation == null) {
            throw new UserException(404, "操作记录不存在");
        }
        return operation;
    }
}
