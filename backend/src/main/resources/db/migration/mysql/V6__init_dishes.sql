CREATE TABLE IF NOT EXISTS dish (
    dish_id             INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    restaurant_id       INT NOT NULL,
    display_order_id    INT NOT NULL,
    price               INT NOT NULL,
    dish_name           VARCHAR(64) NOT NULL,
    CONSTRAINT fk_dish_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (restaurant_id)
);
