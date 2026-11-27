package com.romi.mogumogu.dto.restaurant;

import java.time.LocalDateTime;

import com.romi.mogumogu.entity.RestaurantSelectionEntity;

public record RestaurantSelectionResponse(
        Integer selectionId,
        Integer groupId,
        Integer restaurantId,
        String restaurantName,
        LocalDateTime selectedAt) {

    public static RestaurantSelectionResponse from(RestaurantSelectionEntity selection) {
        return new RestaurantSelectionResponse(
                selection.getSelectionId(),
                selection.getGroupId(),
                selection.getRestaurant().getRestaurantId(),
                selection.getRestaurant().getRestaurantName(),
                selection.getSelectedAt());
    }
}