package com.romi.mogumogu.repository.history;

import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;
import com.romi.mogumogu.testsupport.MemH2DataSourceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class RestaurantSelectionHistoryRepositoryTest {

    @DynamicPropertySource
    static void memH2(DynamicPropertyRegistry registry) {
        MemH2DataSourceProperties.register(registry, "mogu-history-seq-it");
    }

    @Autowired
    private RestaurantSelectionHistoryRepository historyRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantCategoryRepository categoryRepository;

    @Test
    void resetHistoryIdSequence_afterClearingAllRows_nextIdStartsFromOne() {
        RestaurantEntity restaurant = seedRestaurant();
        historyRepository.save(buildHistory(restaurant));
        historyRepository.save(buildHistory(restaurant));
        historyRepository.flush();

        historyRepository.deleteByGroupId(1);
        historyRepository.flush();
        historyRepository.resetHistoryIdSequence();

        RestaurantSelectionHistoryEntity next = historyRepository.save(buildHistory(restaurant));
        historyRepository.flush();

        assertEquals(1, next.getHistoryId());
    }

    private RestaurantEntity seedRestaurant() {
        RestaurantCategoryEntity category = categoryRepository.save(RestaurantCategoryEntity.builder()
                .groupId(1)
                .displayOrderId(1)
                .categoryName("主食")
                .createdAt(new Date())
                .build());
        return restaurantRepository.save(RestaurantEntity.builder()
                .groupId(1)
                .categoryId(category)
                .displayOrderId(1)
                .selectedCount(0)
                .restaurantName("拉麵店")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build());
    }

    private RestaurantSelectionHistoryEntity buildHistory(RestaurantEntity restaurant) {
        return RestaurantSelectionHistoryEntity.builder()
                .groupId(1)
                .restaurant(restaurant)
                .selectedAt(new Date())
                .build();
    }
}
