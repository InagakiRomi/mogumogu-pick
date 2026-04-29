CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id           INT NOT NULL,
    display_order      INT NOT NULL,
    restaurant_name    VARCHAR(64) NOT NULL,
    category           VARCHAR(32) NOT NULL,
    image_url          VARCHAR(512),
    selected_count     INT NOT NULL DEFAULT 0,
    last_selected_at   TIMESTAMP NULL,
    created_at         TIMESTAMP NULL,
    updated_at         TIMESTAMP NULL,
    note               VARCHAR(512)
);
