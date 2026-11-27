package com.romi.mogumogu.dto.restaurant;

import java.time.LocalDateTime;
import com.romi.mogumogu.entity.RestaurantEntity;

public record RestaurantResponse(
        Integer restaurantId,
        Integer groupId,
        Integer categoryId,
        Integer displayOrderId,
        Integer selectedCount,
        String restaurantName,
        String address,
        String note,
        String imageUrl,
        LocalDateTime lastSelectedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static RestaurantResponse from(RestaurantEntity restaurant) {
        return new RestaurantResponse(
                restaurant.getRestaurantId(),
                restaurant.getGroupId(),
                restaurant.getCategoryId(),
                restaurant.getDisplayOrderId(),
                restaurant.getSelectedCount(),
                restaurant.getRestaurantName(),
                restaurant.getAddress(),
                restaurant.getNote(),
                restaurant.getImageUrl(),
                restaurant.getLastSelectedAt(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }
}
