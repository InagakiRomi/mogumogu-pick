package com.romi.mogumogu.dto.restaurant;

public record RestaurantRandomResponse(
        RestaurantResponse restaurant,
        long total,
        Integer remainingCount) {
    public static RestaurantRandomResponse from(RestaurantResponse restaurant, long total, Integer remainingCount) {
        return new RestaurantRandomResponse(
                restaurant,
                total,
                remainingCount);
    }
}