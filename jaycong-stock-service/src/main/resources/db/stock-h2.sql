CREATE TABLE demo_stock (
    product_id BIGINT PRIMARY KEY,
    available_quantity INT NOT NULL CHECK (available_quantity >= 0)
);
INSERT INTO demo_stock (product_id, available_quantity) VALUES (1, 100);
