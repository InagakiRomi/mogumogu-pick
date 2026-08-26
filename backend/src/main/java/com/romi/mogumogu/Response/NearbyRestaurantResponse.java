package com.romi.mogumogu.Response;

import tools.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRestaurantResponse {

    /** OSM ID */
    private Long id;

    /** 餐廳名稱 */
    private String name;

    /** 緯度 */
    private Double latitude;

    /** 經度 */
    private Double longitude;

    /** 料理類型 */
    private String cuisine;

    /** 地址 */
    private String address;

    /** 營業時間 */
    private String openingHours;

    /** 電話 */
    private String phone;

    public static NearbyRestaurantResponse fromOverpass(JsonNode element) {
        JsonNode tags = element.path("tags");

        return NearbyRestaurantResponse.builder()
                .id(element.path("id").asLong())
                .name(tags.path("name").asString(null))
                .latitude(element.path("lat").asDouble())
                .longitude(element.path("lon").asDouble())
                .cuisine(tags.path("cuisine").asString(null))
                .address(tags.path("addr:full").asString(null))
                .openingHours(tags.path("opening_hours").asString(null))
                .phone(tags.path("phone").asString(null))
                .build();
    }
}