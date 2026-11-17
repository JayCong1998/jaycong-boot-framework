-- 仅适用于已执行旧版商品管理脚本且已有 category_id 的数据库。
-- 未执行过旧版脚本请使用 goods-admin-upgrade.sql，不要两个都执行。
-- 执行前备份并确认 category 列不存在；核对旧编号含义与下列固定值一致。
-- 不删除旧 category_id、gallery_urls 或分类表，便于人工核对和恢复。
ALTER TABLE goods ADD COLUMN category VARCHAR(64) NULL COMMENT '商品分类中文值';
UPDATE goods SET category = CASE category_id
    WHEN 1 THEN '手机数码'
    WHEN 2 THEN '电脑办公'
    WHEN 3 THEN '家用电器'
    WHEN 4 THEN '家居生活'
    WHEN 5 THEN '服饰鞋包'
    WHEN 6 THEN '其他商品'
    ELSE NULL END
WHERE category IS NULL;
ALTER TABLE goods ADD KEY idx_goods_category_text_status (category, deleted, status);
