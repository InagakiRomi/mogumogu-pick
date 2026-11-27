package com.romi.mogumogu.dto.restaurant;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record RestaurantMapResponse(
        @Schema(description = "附近餐廳列表") List<Restaurant> restaurants,
        @Schema(description = "找到的餐廳總筆數") Integer total,
        @Schema(description = "目前搜尋緯度") Double latitude,
        @Schema(description = "目前搜尋經度") Double longitude) {
    public record Restaurant(
            @Schema(description = "餐廳名稱") String name,
            @Schema(description = "緯度") Double latitude,
            @Schema(description = "經度") Double longitude,
            @Schema(description = "地址") String address) {
    }

    public static RestaurantMapResponse from(
            List<Restaurant> restaurants,
            double latitude,
            double longitude) {
        return new RestaurantMapResponse(
                restaurants,
                restaurants.size(),
                latitude,
                longitude);
    }
}