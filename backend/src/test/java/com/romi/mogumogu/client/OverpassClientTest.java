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
    @DisplayName("Returns JSON immediately when the first API succeeds")
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
    @DisplayName("Returns JSON with empty elements when the API succeeds without restaurants")
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
    @DisplayName("Falls back to the second API when the first has no response body")
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
    @DisplayName("Uses the third API when the first two have no response body")
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

    @ParameterizedTest(name = "HTTP {0} retries the next Overpass API")
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
    @DisplayName("Transient HTTP errors fall back to the next API")
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
    @DisplayName("Returns successfully when the third API succeeds after two transient failures")
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

    @ParameterizedTest(name = "HTTP {0} aborts immediately and is not retried")
    @ValueSource(ints = {
            400,
            401,
            403,
            404,
            409,
            422,
            499
    })
    @DisplayName("Non-transient HTTP errors return 502 immediately")
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
    @DisplayName("Aborts immediately when the second API returns a non-retryable error")
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
    @DisplayName("Falls back to the second API when the first connection fails")
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
    @DisplayName("Uses the third API when the first two connections fail")
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
    @DisplayName("Falls back to the next API when the first returns invalid JSON")
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
    @DisplayName("Returns 503 when all three APIs fail with transient HTTP errors")
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
    @DisplayName("Returns 503 when all three APIs fail to connect")
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
    @DisplayName("Returns 503 when all three APIs have no response body")
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
    @DisplayName("Returns 503 when mixed HTTP and connection errors all fail")
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

    @ParameterizedTest(name = "latitude = {0} is rejected")
    @MethodSource("invalidLatitudes")
    @DisplayName("Latitude outside the Taiwan range throws ResponseStatusException")
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

    @ParameterizedTest(name = "longitude = {0} is rejected")
    @MethodSource("invalidLongitudes")
    @DisplayName("Longitude outside the Taiwan range throws ResponseStatusException")
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

    @ParameterizedTest(name = "radius = {0} is rejected")
    @ValueSource(ints = {
            -2147483648,
            -1,
            0,
            99,
            3001,
            2147483647
    })
    @DisplayName("Radius below 100 or above 3000 throws ResponseStatusException")
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

    @ParameterizedTest(name = "latitude={0}, longitude={1}, radius={2} is valid")
    @CsvSource({
            "21.7,  118.0,  100",
            "26.5,  122.2,  100",
            "21.7,  122.2,  100",
            "26.5,  118.0,  100",
            "25.0,  121.0,  100",
            "25.0,  121.0,  3000"
    })
    @DisplayName("Coordinate and radius boundary values pass validation")
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
