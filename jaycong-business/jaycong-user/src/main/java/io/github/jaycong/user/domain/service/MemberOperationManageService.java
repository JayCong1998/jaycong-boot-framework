package io.github.jaycong.user.domain.service;

import io.github.jaycong.user.domain.entity.MemberOperation;
import io.github.jaycong.user.domain.entity.User;
import io.github.jaycong.user.infrastructure.mapper.MemberOperationMapper;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 会员变更与阶段流水同事务；Redis 不加入数据库事务，完成结果另行落库。 */
@Service
public class MemberOperationManageService {
    private final MemberOperationMapper mapper;
    private final TransactionTemplate transaction;

    public MemberOperationManageService(MemberOperationMapper mapper, PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public User execute(User actor, long memberId, String action,
            Function<MemberOperation, User> database, Runnable beforeDatabase, Runnable afterDatabase) {
        var operation = MemberOperation.start(actor, memberId, action);
        // 开始记录失败时不执行任何变更，避免无流水的管理操作。
        transaction.executeWithoutResult(status -> {
            if (mapper.insert(operation) != 1) {
                throw new IllegalStateException("操作记录保存失败");
            }
        });
        boolean databaseStarted = false;
        try {
            if (beforeDatabase != null) {
                beforeDatabase.run();
            }
            databaseStarted = true;
            var result = transaction.execute(status -> {
                var user = database.apply(operation);
                operation.databaseCompleted(afterDatabase != null);
                save(operation);
                return user;
            });
            if (afterDatabase != null) {
                afterDatabase.run();
                finish(operation.getId());
            }
            return result;
        } catch (RuntimeException exception) {
            try {
                recordFailure(operation.getId(), exception, databaseStarted);
            } catch (RuntimeException auditFailure) {
                exception.addSuppressed(auditFailure);
            }
            throw exception;
        }
    }

    private void recordFailure(long id, RuntimeException exception, boolean databaseStarted) {
        transaction.executeWithoutResult(status -> {
            // 重新读取，避免回滚后的版本及未提交的变更摘要污染失败记录。
            var persisted = getOperation(id);
            if ("SUCCESS".equals(persisted.getOutcome())) {
                return;
            }
            persisted.recordFailure(exception, databaseStarted);
            save(persisted);
        });
    }

    private void finish(long id) {
        transaction.executeWithoutResult(status -> {
            var operation = getOperation(id);
            operation.complete();
            save(operation);
        });
    }

    private MemberOperation getOperation(long id) {
        var operation = mapper.selectById(id);
        if (operation == null) {
            throw new IllegalStateException("操作记录不存在");
        }
        return operation;
    }

    private void save(MemberOperation operation) {
        if (mapper.updateById(operation) != 1) {
            throw new IllegalStateException("操作记录写入冲突");
        }
    }
}
