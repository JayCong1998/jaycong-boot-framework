CREATE TABLE IF NOT EXISTS test_item (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    create_user_id BIGINT NOT NULL,
    update_user_id BIGINT NOT NULL,
    deleted TINYINT NOT NULL,
    lock_version INT NOT NULL
);
