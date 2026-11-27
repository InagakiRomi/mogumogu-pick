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

    @Schema(description = "找到的餐廳總筆數")
    private Long total;

    @Schema(description = "目前搜尋緯度")
    private Double latitude;

    @Schema(description = "目前搜尋經度")
    private Double longitude;
}
