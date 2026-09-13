package com.romi.mogumogu.response;

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

    /** 地址 */
    private String address;

    /** 營業時間 */
    private String openingHours;

    /** 電話 */
    private String phone;
}
