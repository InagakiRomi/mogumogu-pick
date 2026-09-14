package com.romi.mogumogu.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRestaurantSearchResponse {

    @Schema(description = "附近餐廳列表")
    private List<NearbyRestaurantResponse> restaurants;

    @Schema(description = "找到的餐廳數", example = "12")
    private Integer restaurantCount;

    @Schema(description = "目前搜尋緯度", example = "25.033")
    private Double latitude;

    @Schema(description = "目前搜尋經度", example = "121.5654")
    private Double longitude;

    public static NearbyRestaurantSearchResponse of(
            List<NearbyRestaurantResponse> restaurants,
            double latitude,
            double longitude) {
        return NearbyRestaurantSearchResponse.builder()
                .restaurants(restaurants)
                .restaurantCount(restaurants.size())
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
