package com.romi.mogumogu.mapper;

import com.romi.mogumogu.response.NearbyRestaurantResponse;
import com.romi.mogumogu.util.RestaurantDataFormatter;

import tools.jackson.databind.JsonNode;

public class NearbyRestaurantMapper {

    private NearbyRestaurantMapper() {
    }

    /** 將 Overpass API 資料轉成 DTO */
    public static NearbyRestaurantResponse fromOverpass(JsonNode element, String name) {
        JsonNode tags = element.path("tags");

        Double latitude;
        Double longitude;

        // 取得座標
        if (element.has("lat") && element.has("lon")) {
            latitude = element.path("lat").asDouble();
            longitude = element.path("lon").asDouble();
        } else if (element.has("center")) {
            JsonNode center = element.path("center");

            if (!center.has("lat") || !center.has("lon")) {
                return null;
            }

            latitude = center.path("lat").asDouble();
            longitude = center.path("lon").asDouble();
        } else {
            return null;
        }

        return NearbyRestaurantResponse.builder()
                .id(element.path("id").asLong())
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .address(getAddress(tags))
                .openingHours(RestaurantDataFormatter.formatOpeningHours(
                        tags.path("opening_hours").asString(null)))
                .phone(RestaurantDataFormatter.formatPhone(getPhone(tags)))
                .build();
    }

    /** 取得地址 */
    private static String getAddress(JsonNode tags) {
        String fullAddress = tags.path("addr:full").asString(null);

        if (fullAddress != null && !fullAddress.isBlank()) {
            return fullAddress;
        }

        StringBuilder address = new StringBuilder();

        for (String key : new String[]{
                "addr:city",
                "addr:district",
                "addr:street",
                "addr:housenumber"
        }) {
            String value = tags.path(key).asString(null);

            if (value != null && !value.isBlank()) {
                address.append(value);
            }
        }

        return address.isEmpty() ? null : address.toString();
    }

    /** 取得電話 */
    private static String getPhone(JsonNode tags) {
        for (String key : new String[]{"phone", "contact:phone"}) {
            String phone = tags.path(key).asString(null);

            if (phone != null && !phone.isBlank()) {
                return phone;
            }
        }

        return null;
    }
}
