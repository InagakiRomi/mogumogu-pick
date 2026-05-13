ALTER TABLE restaurant_category
    CHANGE COLUMN display_order display_order_id INT NOT NULL;

ALTER TABLE restaurant
    CHANGE COLUMN display_order display_order_id INT NOT NULL;
