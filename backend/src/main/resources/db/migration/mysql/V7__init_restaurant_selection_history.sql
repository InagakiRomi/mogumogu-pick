CREATE TABLE IF NOT EXISTS restaurant_selection_history (
    history_id       INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id         INT NOT NULL,
    restaurant_id    INT NOT NULL,
    selected_at      TIMESTAMP NOT NULL,
    CONSTRAINT fk_selection_history_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (restaurant_id)
);

CREATE INDEX idx_selection_history_group_selected
    ON restaurant_selection_history (group_id, selected_at DESC);
