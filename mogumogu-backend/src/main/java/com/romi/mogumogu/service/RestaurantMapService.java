package com.romi.mogumogu.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.restaurant.OverpassResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantMapResponse;

@Service
public class RestaurantMapService {
    private final RestClient restClient;

    /** 初始化 RestClient */
    public RestaurantMapService(RestClient.Builder builder) {
        this.restClient = builder
                // 設定外部API基礎網址
                .baseUrl("https://overpass-api.de")

                // 設定發送請求的應用程式名稱
                .defaultHeader("User-Agent", "MoguMogu/1.0")

                // 建立 RestClient
                .build();
    }

    /** 取得附近餐廳資料 */
    public RestaurantMapResponse getMapRestaurants(double lat, double lon, int radius) {
        // 呼叫外部 API 取得附近餐廳資料
        OverpassResponse response = searchNearbyRestaurants(lat, lon, radius);

        // 將外部 API 資料轉換成餐廳清單
        List<RestaurantMapResponse.Restaurant> restaurants = response.elements().stream()
                .map(this::toRestaurant)

                // 排除沒有完整經緯度的餐廳
                .filter(restaurant -> restaurant.latitude() != null
                        && restaurant.longitude() != null)

                // 轉換成 List
                .toList();

        // 包裝成地圖餐廳 Response DTO
        return RestaurantMapResponse.from(restaurants, lat, lon);
    }

    /** 搜尋附近餐廳 */
    private OverpassResponse searchNearbyRestaurants(double lat, double lon, int radius) {
        // 建立Overpass API查詢語法
        String query = """
                [out:json][timeout:25];
                nwr["amenity"~"^(restaurant|fast_food|cafe|food_court|ice_cream)$"]["name"]
                (around:%d,%s,%s);
                out center;
                """.formatted(radius, lat, lon);

        OverpassResponse response;

        try {
            // 發送 GET 請求取得附近餐廳資料
            response = restClient.get()
                    // 設定 API 路徑及查詢參數
                    .uri("/api/interpreter?data={query}", query)

                    // 指定接收 JSON 格式
                    .accept(MediaType.APPLICATION_JSON)

                    // 取得 API 回應
                    .retrieve()

                    // 將 JSON 資料轉換成 OverpassResponse
                    .body(OverpassResponse.class);
        } catch (RestClientException e) {
            // 外部 API 請求失敗
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "無法取得外部餐廳資料", e);
        }

        // 檢查外部 API 是否回傳異常資料
        if (response == null || response.elements() == null || response.remark() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "外部餐廳資料回傳異常");
        }

        // 回傳外部 API 資料
        return response;
    }

    /** 將外部API資料轉換成地圖DTO */
    private RestaurantMapResponse.Restaurant toRestaurant(OverpassResponse.Element element) {
        // 取得餐廳標籤資料
        Map<String, String> tags = element.tags();

        // 如果沒有標籤資料，使用空Map避免發生空指標例外
        if (tags == null) {
            tags = Map.of();
        }

        // 取得餐廳名稱
        String name = tags.get("name");

        // 取得餐廳原始經緯度
        Double latitude = element.lat();
        Double longitude = element.lon();

        // 如果有中心座標，改用中心座標
        if (element.center() != null) {
            latitude = element.center().lat();
            longitude = element.center().lon();
        }

        // 取得餐廳地址
        String address = getAddress(tags);

        // 建立餐廳Response DTO
        return new RestaurantMapResponse.Restaurant(name, latitude, longitude, address);
    }

    /** 組合餐廳地址 */
    private String getAddress(Map<String, String> tags) {

        // 如果有完整地址，直接回傳
        if (tags.containsKey("addr:full")) {
            return tags.get("addr:full");
        }

        // 取得各部分地址資料
        String city = tags.get("addr:city");
        String district = tags.get("addr:district");
        String street = tags.get("addr:street");
        String houseNumber = tags.get("addr:housenumber");

        // 組合完整地址
        String address = Stream.of(city, district, street, houseNumber)
                // 排除 null 資料
                .filter(Objects::nonNull)

                // 排除空白字串
                .filter(value -> !value.isBlank())

                // 將各部分地址合併成字串
                .collect(Collectors.joining(""));

        // 如果沒有地址資料，回傳 null
        return address.isBlank() ? null : address;
    }
}