package com.romi.mogumogu.testsupport;

import java.util.Date;

import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;

/** 整合測試共用的種子資料建立工具。 */
public final class IntegrationTestFixtures {

    private IntegrationTestFixtures() {}

    /** 建立群組 1 的餐廳分類，供餐廳 CRUD 整合測試使用。 */
    @SuppressWarnings("null")
    public static RestaurantCategoryEntity seedCategoryGroup1(RestaurantCategoryRepository repository) {
        return repository.save(RestaurantCategoryEntity.builder()
                .groupId(1)
                .displayOrderId(1)
                .categoryName("和食")
                .createdAt(new Date())
                .build());
    }
}
