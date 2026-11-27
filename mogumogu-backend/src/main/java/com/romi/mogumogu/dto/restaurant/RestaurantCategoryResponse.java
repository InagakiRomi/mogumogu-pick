package com.romi.mogumogu.dto.restaurant;

import java.time.LocalDateTime;

import com.romi.mogumogu.entity.RestaurantCategoryEntity;

public record RestaurantCategoryResponse(
        Integer categoryId,
        Integer groupId,
        Integer displayOrderId,
        String categoryName,
        LocalDateTime createdAt) {
    public static RestaurantCategoryResponse from(RestaurantCategoryEntity restaurantCategory) {
        return new RestaurantCategoryResponse(
                restaurantCategory.getCategoryId(),
                restaurantCategory.getGroupId(),
                restaurantCategory.getDisplayOrderId(),
                restaurantCategory.getCategoryName(),
                restaurantCategory.getCreatedAt());
    }
}
