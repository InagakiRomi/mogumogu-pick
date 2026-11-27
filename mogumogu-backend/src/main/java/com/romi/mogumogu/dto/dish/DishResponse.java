package com.romi.mogumogu.dto.dish;

import com.romi.mogumogu.entity.DishEntity;

public record DishResponse(
        Integer dishId,
        Integer restaurantId,
        Integer displayOrderId,
        Integer price,
        String dishName) {

    public static DishResponse from(DishEntity dish) {
        return new DishResponse(
                dish.getDishId(),
                dish.getRestaurantId(),
                dish.getDisplayOrderId(),
                dish.getPrice(),
                dish.getDishName());
    }
}