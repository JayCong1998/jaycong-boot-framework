-- 在独立的 jaycong_order 数据库执行；不删除已有数据。
CREATE TABLE IF NOT EXISTS demo_order (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT chk_order_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
