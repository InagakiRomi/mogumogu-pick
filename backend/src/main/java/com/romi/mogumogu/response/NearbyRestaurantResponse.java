package com.romi.mogumogu.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRestaurantResponse {
    @Schema(description = "OSM ID")
    private Long id;

    @Schema(description = "餐廳名稱")
    private String name;

    @Schema(description = "緯度")
    private Double latitude;

    @Schema(description = "經度")
    private Double longitude;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "營業時間")
    private String openingHours;

    @Schema(description = "電話")
    private String phone;
}
