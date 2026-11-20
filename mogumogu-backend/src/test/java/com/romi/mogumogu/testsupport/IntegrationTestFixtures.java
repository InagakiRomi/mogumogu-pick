package com.romi.mogumogu.testsupport;

import java.util.Date;

import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;

public final class IntegrationTestFixtures {

    private IntegrationTestFixtures() {
    }

    public static RestaurantCategoryEntity seedCategoryGroup1(RestaurantCategoryRepository repository) {
        return repository.save(RestaurantCategoryEntity.builder()
                .groupId(1)
                .displayOrderId(1)
                .categoryName("和食")
                .createdAt(new Date())
                .build());
    }
}
