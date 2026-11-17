-- 已有 goods 表的数据库手工执行本脚本；全新数据库执行 goods-mysql.sql 即可，不要两个都执行。
-- 执行前备份并确认新增列和索引尚不存在；本脚本只执行一次。
-- 不修改现有商品资料、库存及状态。旧商品未分类，可在后台编辑时选择分类。
ALTER TABLE goods
    ADD COLUMN category VARCHAR(64) NULL COMMENT '商品分类中文值，旧商品允许未分类',
    ADD COLUMN original_price BIGINT NULL COMMENT '划线价，单位为人民币分',
    ADD COLUMN stock_warning INT NOT NULL DEFAULT 0 COMMENT '库存预警阈值',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    ADD KEY idx_goods_category_status (category, deleted, status);
