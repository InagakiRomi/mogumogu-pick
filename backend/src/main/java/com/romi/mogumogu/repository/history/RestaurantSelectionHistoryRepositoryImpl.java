package com.romi.mogumogu.repository.history;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;

import java.util.Locale;

public class RestaurantSelectionHistoryRepositoryImpl implements RestaurantSelectionHistoryRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void resetHistoryIdSequence() {
        Number maxId = (Number) entityManager
                .createQuery("SELECT COALESCE(MAX(h.historyId), 0) FROM RestaurantSelectionHistoryEntity h")
                .getSingleResult();
        int nextValue = maxId.intValue() + 1;
        boolean mysql = entityManager.unwrap(Session.class).doReturningWork(connection -> {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
        });
        String sql = mysql
                ? "ALTER TABLE restaurant_selection_history AUTO_INCREMENT = " + nextValue
                : "ALTER TABLE restaurant_selection_history ALTER COLUMN history_id RESTART WITH " + nextValue;
        entityManager.createNativeQuery(sql).executeUpdate();
    }
}
