package com.romi.mogumogu.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;

class OverpassClientTest {

    private static final String ENDPOINT_1 = "https://maps.mail.ru/osm/tools/overpass/api/interpreter";

    private static final String ENDPOINT_2 = "https://overpass.private.coffee/api/interpreter";

    private static final String ENDPOINT_3 = "https://overpass-api.de/api/interpreter";

    private static final double LATITUDE = 25.0330;
    private static final double LONGITUDE = 121.5654;
    private static final int RADIUS = 1000;

    private static final String EXPECTED_QUERY = """
            [out:json][timeout:25];
            (
              nwr["amenity"="restaurant"]["name"](around:1000,25.033000,121.565400);
              nwr["amenity"="cafe"]["name"](around:1000,25.033000,121.565400);
              nwr["amenity"="fast_food"]["name"](around:1000,25.033000,121.565400);
              nwr["amenity"="food_court"]["name"](around:1000,25.033000,121.565400);
            );
            out center tags qt;
            """;

    private static final String SUCCESS_JSON = """
            {
              "version": 0.6,
              "generator": "Overpass API",
              "elements": [
                {
                  "type": "node",
                  "id": 123456,
                  "lat": 25.0331,
                  "lon": 121.5655,
                  "tags": {
                    "amenity": "restaurant",
                    "name": "測試餐廳"
                  }
                }
              ]
            }
            """;

    private static final String EMPTY_ELEMENTS_JSON = """
            {
              "version": 0.6,
              "elements": []
            }
            """;

    private MockRestServiceServer server;

    private OverpassClient overpassClient;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        RestClient restClient = builder.build();

        overpassClient = new OverpassClient(restClient);
    }

    @Test
    @DisplayName("第一個 API 成功時，應直接回傳 JSON")
    void searchRestaurants_firstEndpointSuccess_shouldReturnJson() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                1,
                result.path("elements").size());

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("API 成功但沒有餐廳時，應回傳 elements 為空的 JSON")
    void searchRestaurants_emptyElements_shouldStillReturnJson() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withSuccess(
                                EMPTY_ELEMENTS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                0,
                result.path("elements").size());

        server.verify();
    }

    @Test
    @DisplayName("第一個 API 沒有 response body 時，應改用第二個 API")
    void searchRestaurants_firstEndpointEmpty_shouldTrySecondEndpoint() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withNoContent());

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("前兩個 API 都沒有 response body 時，應使用第三個 API")
    void searchRestaurants_firstTwoEmpty_shouldTryThirdEndpoint() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withNoContent());

        expectStandardRequest(ENDPOINT_2)
                .andRespond(withNoContent());

        expectStandardRequest(ENDPOINT_3)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @ParameterizedTest(name = "HTTP {0} 應重試下一個 Overpass API")
    @ValueSource(ints = {
            408,
            429,
            500,
            501,
            502,
            503,
            504,
            599
    })
    @DisplayName("暫時性 HTTP 錯誤應改用下一個 API")
    void searchRestaurants_retryableStatus_shouldTryNextEndpoint(
            int status) {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(status));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("前兩個 API 暫時失敗時，第三個成功應正常回傳")
    void searchRestaurants_firstTwoRetryableErrors_thirdShouldSucceed() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(503));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(withRawStatus(429));

        expectStandardRequest(ENDPOINT_3)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @ParameterizedTest(name = "HTTP {0} 應直接中止，不可重試")
    @ValueSource(ints = {
            400,
            401,
            403,
            404,
            409,
            422,
            499
    })
    @DisplayName("非暫時性 HTTP 錯誤應直接回傳 502")
    void searchRestaurants_nonRetryableStatus_shouldThrowBadGateway(
            int status) {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(status));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                exception.getStatusCode());

        assertEquals(
                "Overpass API rejected request",
                exception.getReason());

        RestClientResponseException cause = assertInstanceOf(
                RestClientResponseException.class,
                exception.getCause());

        assertEquals(
                status,
                cause.getStatusCode().value());

        server.verify();
    }

    @Test
    @DisplayName("第一個 API 可重試，但第二個 API 回傳不可重試錯誤時應立即中止")
    void searchRestaurants_secondEndpointNonRetryable_shouldStopImmediately() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(503));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(withRawStatus(400));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                exception.getStatusCode());

        RestClientResponseException cause = assertInstanceOf(
                RestClientResponseException.class,
                exception.getCause());

        assertEquals(
                400,
                cause.getStatusCode().value());

        server.verify();
    }

    @Test
    @DisplayName("第一個 API 連線失敗時，應改用第二個 API")
    void searchRestaurants_connectionFailure_shouldTryNextEndpoint() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Connection timeout")));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("前兩個 API 連線失敗時，應使用第三個 API")
    void searchRestaurants_firstTwoConnectionFailures_thirdShouldSucceed() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 1 timeout")));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 2 timeout")));

        expectStandardRequest(ENDPOINT_3)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("第一個 API 回傳錯誤 JSON 時，應改用下一個 API")
    void searchRestaurants_invalidJson_shouldTryNextEndpoint() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withSuccess(
                                "{ invalid json",
                                MediaType.APPLICATION_JSON));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withSuccess(
                                SUCCESS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = overpassClient.searchRestaurants(
                LATITUDE,
                LONGITUDE,
                RADIUS);

        assertNotNull(result);

        assertEquals(
                "測試餐廳",
                result
                        .path("elements")
                        .get(0)
                        .path("tags")
                        .path("name")
                        .asString());

        server.verify();
    }

    @Test
    @DisplayName("三個 API 都是暫時性 HTTP 錯誤時，應回傳 503")
    void searchRestaurants_allRetryableHttpErrors_shouldThrowServiceUnavailable() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(408));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(withRawStatus(429));

        expectStandardRequest(ENDPOINT_3)
                .andRespond(withRawStatus(503));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());

        assertEquals(
                "Overpass API temporarily unavailable",
                exception.getReason());

        RestClientResponseException cause = assertInstanceOf(
                RestClientResponseException.class,
                exception.getCause());

        assertEquals(
                503,
                cause.getStatusCode().value());

        server.verify();
    }

    @Test
    @DisplayName("三個 API 全部連線失敗時，應回傳 503")
    void searchRestaurants_allConnectionFailures_shouldThrowServiceUnavailable() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 1 timeout")));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 2 timeout")));

        expectStandardRequest(ENDPOINT_3)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 3 timeout")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());

        assertEquals(
                "Overpass API temporarily unavailable",
                exception.getReason());

        assertInstanceOf(
                RestClientException.class,
                exception.getCause());

        server.verify();
    }

    @Test
    @DisplayName("三個 API 都沒有 response body 時，應回傳 503")
    void searchRestaurants_allEmptyResponses_shouldThrowServiceUnavailable() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withNoContent());

        expectStandardRequest(ENDPOINT_2)
                .andRespond(withNoContent());

        expectStandardRequest(ENDPOINT_3)
                .andRespond(withNoContent());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());

        assertEquals(
                "Overpass API temporarily unavailable",
                exception.getReason());

        assertNull(exception.getCause());

        server.verify();
    }

    @Test
    @DisplayName("HTTP 錯誤、連線錯誤混合且全部失敗時，應回傳 503")
    void searchRestaurants_mixedFailures_shouldThrowServiceUnavailable() {

        expectStandardRequest(ENDPOINT_1)
                .andRespond(withRawStatus(503));

        expectStandardRequest(ENDPOINT_2)
                .andRespond(
                        withException(
                                new SocketTimeoutException(
                                        "Endpoint 2 timeout")));

        expectStandardRequest(ENDPOINT_3)
                .andRespond(withRawStatus(429));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        RADIUS));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());

        RestClientResponseException cause = assertInstanceOf(
                RestClientResponseException.class,
                exception.getCause());

        assertEquals(
                429,
                cause.getStatusCode().value());

        server.verify();
    }

    @ParameterizedTest(name = "latitude = {0} 應被拒絕")
    @MethodSource("invalidLatitudes")
    @DisplayName("Latitude 超出台灣範圍時應丟 ResponseStatusException")
    void searchRestaurants_invalidLatitude_shouldThrowResponseStatusException(
            double latitude) {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        latitude,
                        LONGITUDE,
                        RADIUS));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Latitude must be between 21.7 and 26.5",
                exception.getReason());

        server.verify();
    }

    static Stream<Double> invalidLatitudes() {

        return Stream.of(
                21.699999,
                26.500001,
                0.0,
                90.0,
                -90.0,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN);
    }

    @ParameterizedTest(name = "longitude = {0} 應被拒絕")
    @MethodSource("invalidLongitudes")
    @DisplayName("Longitude 超出台灣範圍時應丟 ResponseStatusException")
    void searchRestaurants_invalidLongitude_shouldThrowResponseStatusException(
            double longitude) {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        longitude,
                        RADIUS));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Longitude must be between 118.0 and 122.2",
                exception.getReason());

        server.verify();
    }

    static Stream<Double> invalidLongitudes() {

        return Stream.of(
                117.999999,
                122.200001,
                0.0,
                180.0,
                -180.0,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN);
    }

    @ParameterizedTest(name = "radius = {0} 應被拒絕")
    @ValueSource(ints = {
            -2147483648,
            -1,
            0,
            99,
            3001,
            2147483647
    })
    @DisplayName("Radius 小於 100 或大於 3000 時應丟 ResponseStatusException")
    void searchRestaurants_invalidRadius_shouldThrowResponseStatusException(
            int radius) {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> overpassClient.searchRestaurants(
                        LATITUDE,
                        LONGITUDE,
                        radius));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Radius must be between 100 and 3000 meters",
                exception.getReason());

        server.verify();
    }

    @ParameterizedTest(name = "latitude={0}, longitude={1}, radius={2} 應合法")
    @CsvSource({
            "21.7,  118.0,  100",
            "26.5,  122.2,  100",
            "21.7,  122.2,  100",
            "26.5,  118.0,  100",
            "25.0,  121.0,  100",
            "25.0,  121.0,  3000"
    })
    @DisplayName("座標與 radius 邊界值應通過驗證")
    void searchRestaurants_boundaryValues_shouldBeAccepted(
            double latitude,
            double longitude,
            int radius) {

        expectBasicRequest(ENDPOINT_1)
                .andRespond(
                        withSuccess(
                                EMPTY_ELEMENTS_JSON,
                                MediaType.APPLICATION_JSON));

        JsonNode result = assertDoesNotThrow(
                () -> overpassClient.searchRestaurants(
                        latitude,
                        longitude,
                        radius));

        assertNotNull(result);

        server.verify();
    }

    private ResponseActions expectBasicRequest(
            String endpoint) {

        return server
                .expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(
                        header(
                                HttpHeaders.ACCEPT,
                                MediaType.APPLICATION_JSON_VALUE));
    }

    private ResponseActions expectStandardRequest(
            String endpoint) {

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();

        expectedForm.add(
                "data",
                EXPECTED_QUERY);

        return expectBasicRequest(endpoint)
                .andExpect(
                        content()
                                .formData(expectedForm));
    }
}
