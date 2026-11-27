CREATE TABLE `group` (
    group_id      INT AUTO_INCREMENT PRIMARY KEY,
    group_name    VARCHAR(32) NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE TABLE `user` (
    user_id              INT AUTO_INCREMENT PRIMARY KEY,
    group_id             INT,
    display_order_id     INT NOT NULL,
    `role`               INT NOT NULL DEFAULT 1,
    username             VARCHAR(32) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `group` (group_id)
);

CREATE TABLE restaurant_category (
    category_id      INT AUTO_INCREMENT PRIMARY KEY,
    group_id         INT NOT NULL,
    display_order_id INT NOT NULL,
    category_name    VARCHAR(32) NOT NULL,
    created_at       TIMESTAMP,
    UNIQUE (group_id, category_name),
    FOREIGN KEY (group_id) REFERENCES `group` (group_id)
);

CREATE TABLE restaurant (
    restaurant_id      INT AUTO_INCREMENT PRIMARY KEY,
    group_id           INT NOT NULL,
    category_id        INT NOT NULL,
    display_order_id   INT NOT NULL,
    selected_count     INT NOT NULL DEFAULT 0,
    restaurant_name    VARCHAR(255) NOT NULL,
    address            VARCHAR(255),
    note               VARCHAR(512),
    image_url          VARCHAR(512),
    last_selected_at   TIMESTAMP,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `group` (group_id),
    FOREIGN KEY (category_id) REFERENCES restaurant_category (category_id)
);

CREATE TABLE dish (
    dish_id             INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id       INT NOT NULL,
    display_order_id    INT NOT NULL,
    price               INT NOT NULL,
    dish_name           VARCHAR(64) NOT NULL,
    FOREIGN KEY (restaurant_id) REFERENCES restaurant (restaurant_id)
);

CREATE TABLE restaurant_selection (
    selection_id     INT AUTO_INCREMENT PRIMARY KEY,
    group_id         INT NOT NULL,
    restaurant_id    INT NOT NULL,
    selected_at      TIMESTAMP NOT NULL,
    FOREIGN KEY (group_id) REFERENCES `group` (group_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurant (restaurant_id)
);