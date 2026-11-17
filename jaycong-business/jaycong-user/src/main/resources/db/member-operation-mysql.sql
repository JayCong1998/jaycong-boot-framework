-- 手工执行；不回填历史流水，不修改 sys_user。
CREATE TABLE IF NOT EXISTS member_operation_log (
    id BIGINT NOT NULL COMMENT '应用生成主键',
    member_id BIGINT NOT NULL COMMENT '目标会员ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人昵称快照',
    operator_type VARCHAR(16) NOT NULL COMMENT '操作身份 ADMIN MEMBER',
    source VARCHAR(32) NOT NULL COMMENT '来源 ADMIN_WEB MEMBER_WEB',
    action VARCHAR(32) NOT NULL COMMENT '操作类型',
    outcome VARCHAR(16) NOT NULL COMMENT 'PROCESSING SUCCESS FAILED PARTIAL',
    before_value VARCHAR(255) NULL COMMENT '已提交变更前值',
    after_value VARCHAR(255) NULL COMMENT '已提交变更后值',
    message VARCHAR(255) NOT NULL COMMENT '安全结果摘要',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    update_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    lock_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_member_time (member_id, create_time, id),
    KEY idx_operator_time (operator_id, create_time, id),
    KEY idx_time (create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员操作审计流水';
