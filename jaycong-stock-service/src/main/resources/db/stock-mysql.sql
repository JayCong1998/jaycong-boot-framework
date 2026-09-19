-- 在独立的 jaycong_stock 数据库执行；重复执行不会重置已有库存。
CREATE TABLE IF NOT EXISTS demo_stock (
    product_id BIGINT NOT NULL PRIMARY KEY,
    available_quantity INT NOT NULL,
    CONSTRAINT chk_stock_quantity CHECK (available_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO demo_stock (product_id, available_quantity) VALUES (1, 100)
ON DUPLICATE KEY UPDATE product_id = 1;
