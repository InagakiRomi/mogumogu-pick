CREATE TABLE IF NOT EXISTS `user` (
    user_id              INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id             INT NOT NULL,
    display_order_id     INT NOT NULL,
    username             VARCHAR(64) NOT NULL,
    user_password        VARCHAR(255) NOT NULL,
    roles                INT NOT NULL DEFAULT 2,
    created_at           TIMESTAMP NULL,
    updated_at           TIMESTAMP NULL,
    email                VARCHAR(255) NOT NULL,
    UNIQUE KEY uk_users_email (email)
);
