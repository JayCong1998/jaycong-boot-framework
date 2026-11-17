-- 全新数据库在目标库中手工执行，不由应用自动建表。
-- 已有 goods 表请改用 goods-admin-upgrade.sql；IF NOT EXISTS 不会升级现有表结构。
CREATE TABLE IF NOT EXISTS goods (
    id BIGINT NOT NULL COMMENT '商品ID，由应用生成',
    goods_code VARCHAR(64) NOT NULL COMMENT '商品编码，创建后保持稳定',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    subtitle VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品副标题',
    cover_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '商品封面地址',
    description TEXT COMMENT '商品描述',
    price BIGINT NOT NULL COMMENT '售价，单位为人民币分',
    stock INT NOT NULL DEFAULT 0 COMMENT '可售库存',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态：0下架，1上架',
    category VARCHAR(64) NULL COMMENT '商品分类中文值，旧商品允许未分类',
    original_price BIGINT NULL COMMENT '划线价，单位为人民币分',
    stock_warning INT NOT NULL DEFAULT 0 COMMENT '库存预警阈值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间，由应用维护',
    create_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0表示系统操作',
    update_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0表示系统操作',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    lock_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_code (goods_code),
    KEY idx_goods_deleted_status_create_time_id (deleted, status, create_time, id),
    KEY idx_goods_category_status (category, deleted, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商城商品表';
