ALTER TABLE `user`
    MODIFY COLUMN roles INT NOT NULL DEFAULT 2 AFTER display_order_id;
