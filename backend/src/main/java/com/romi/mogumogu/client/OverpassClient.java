package com.romi.mogumogu.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;

@Component
public class OverpassClient {

        private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

        // Overpass API 主站與備援站
        private static final List<String> ENDPOINTS = List.of(
                        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
                        "https://overpass.private.coffee/api/interpreter",
                        "https://overpass-api.de/api/interpreter");

        // 搜尋半徑限制
        private static final int MIN_RADIUS = 100;
        private static final int MAX_RADIUS = 3000;

        // 台灣搜尋範圍（西南 21.7, 118.0；東北 26.5, 122.2）
        private static final double MIN_LATITUDE = 21.7;
        private static final double MAX_LATITUDE = 26.5;
        private static final double MIN_LONGITUDE = 118.0;
        private static final double MAX_LONGITUDE = 122.2;

        private final RestClient restClient;

        public OverpassClient(
                        @Qualifier("overpassRestClient") RestClient restClient) {

                this.restClient = restClient;
        }

        /** 搜尋指定位置附近的餐廳 */
        public JsonNode searchRestaurants(
                        double latitude,
                        double longitude,
                        int radius) {

                // 驗證座標與搜尋半徑
                validateCoordinates(latitude, longitude);
                validateRadius(radius);

                // 建立 Overpass 查詢語法
                String query = createRestaurantQuery(
                                latitude,
                                longitude,
                                radius);

                // 建立 API 表單資料
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

                formData.add("data", query);

                RestClientException lastException = null;

                // 依序嘗試 Overpass API
                for (String endpoint : ENDPOINTS) {

                        try {
                                JsonNode response = restClient
                                                .post()
                                                .uri(endpoint)
                                                .contentType(
                                                                MediaType.APPLICATION_FORM_URLENCODED)
                                                .accept(MediaType.APPLICATION_JSON)
                                                .body(formData)
                                                .retrieve()
                                                .body(JsonNode.class);

                                // 回傳成功取得的資料
                                if (response != null) {
                                        return response;
                                }

                                log.warn(
                                                "Overpass API returned empty response. endpoint={}",
                                                endpoint);

                        } catch (RestClientResponseException ex) {

                                lastException = ex;

                                log.warn(
                                                "Overpass API HTTP error. endpoint={}, status={}, response={}",
                                                endpoint,
                                                ex.getStatusCode(),
                                                ex.getResponseBodyAsString());

                                // 暫時性錯誤則改用下一個 API
                                if (isRetryable(ex)) {
                                        continue;
                                }

                                // 非暫時性錯誤直接中止請求
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "Overpass API rejected request",
                                                ex);

                        } catch (RestClientException ex) {

                                lastException = ex;

                                // 連線失敗時改用下一個 API
                                log.warn(
                                                "Overpass API connection failed. endpoint={}",
                                                endpoint,
                                                ex);
                        }
                }

                // 所有 API 都失敗時回傳服務不可用
                throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Overpass API temporarily unavailable",
                                lastException);
        }

        /** 建立餐廳搜尋條件 */
        private String createRestaurantQuery(
                        double latitude,
                        double longitude,
                        int radius) {

                return """
                                [out:json][timeout:25];
                                (
                                  nwr["amenity"="restaurant"]["name"](around:%d,%f,%f);
                                  nwr["amenity"="cafe"]["name"](around:%d,%f,%f);
                                  nwr["amenity"="fast_food"]["name"](around:%d,%f,%f);
                                  nwr["amenity"="food_court"]["name"](around:%d,%f,%f);
                                );
                                out center tags qt;
                                """.formatted(
                                radius, latitude, longitude,
                                radius, latitude, longitude,
                                radius, latitude, longitude,
                                radius, latitude, longitude);
        }

        /** 判斷是否改用下一個 API */
        private boolean isRetryable(
                        RestClientResponseException ex) {

                int status = ex.getStatusCode().value();

                return status == 408
                                || status == 429
                                || status >= 500;
        }

        /** 驗證經緯度範圍 */
        private void validateCoordinates(
                        double latitude,
                        double longitude) {

                if (!Double.isFinite(latitude)
                                || latitude < MIN_LATITUDE
                                || latitude > MAX_LATITUDE) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Latitude must be between "
                                                        + MIN_LATITUDE
                                                        + " and "
                                                        + MAX_LATITUDE);
                }

                if (!Double.isFinite(longitude)
                                || longitude < MIN_LONGITUDE
                                || longitude > MAX_LONGITUDE) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Longitude must be between "
                                                        + MIN_LONGITUDE
                                                        + " and "
                                                        + MAX_LONGITUDE);
                }
        }

        /** 驗證搜尋半徑範圍 */
        private void validateRadius(int radius) {

                if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Radius must be between "
                                                        + MIN_RADIUS
                                                        + " and "
                                                        + MAX_RADIUS
                                                        + " meters");
                }
        }
}