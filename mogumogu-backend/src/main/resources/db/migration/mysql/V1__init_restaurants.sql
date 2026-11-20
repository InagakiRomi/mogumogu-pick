CREATE TABLE IF NOT EXISTS restaurant_category (
    category_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id         INT NOT NULL,
    display_order    INT NOT NULL,
    category_name    VARCHAR(32) NOT NULL,
    created_at       TIMESTAMP NULL,
    CONSTRAINT uk_restaurant_category_group_name UNIQUE (group_id, category_name)
);

CREATE TABLE IF NOT EXISTS restaurant (
    restaurant_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id           INT NOT NULL,
    category_id        INT NOT NULL,
    display_order      INT NOT NULL,
    selected_count     INT NOT NULL DEFAULT 0,
    restaurant_name    VARCHAR(64) NOT NULL,
    note               VARCHAR(512),
    image_url          VARCHAR(512),
    is_archived        BOOLEAN NOT NULL DEFAULT FALSE,
    last_selected_at   TIMESTAMP NULL,
    created_at         TIMESTAMP NULL,
    updated_at         TIMESTAMP NULL,
    CONSTRAINT fk_restaurants_category
        FOREIGN KEY (category_id) REFERENCES restaurant_category (category_id)
);
