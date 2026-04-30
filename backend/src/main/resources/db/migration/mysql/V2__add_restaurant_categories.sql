CREATE TABLE IF NOT EXISTS restaurant_categories (
    category_id      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id         INT NOT NULL,
    display_order    INT NOT NULL,
    category_name    VARCHAR(32) NOT NULL,
    CONSTRAINT uk_restaurant_categories_group_name UNIQUE (group_id, category_name)
);

ALTER TABLE restaurants
    CHANGE COLUMN category category_id VARCHAR(32) NOT NULL AFTER group_id;
