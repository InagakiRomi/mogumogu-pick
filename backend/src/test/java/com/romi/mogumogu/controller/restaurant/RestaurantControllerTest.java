package com.romi.mogumogu.controller.restaurant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.DishListResponse;
import com.romi.mogumogu.Response.DishResponse;
import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.Response.SelectionHistoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
import com.romi.mogumogu.dto.GetSelectionHistoryQuery;
import com.romi.mogumogu.dto.UpdateRestaurantDto;
import com.romi.mogumogu.enums.RestaurantSort;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import com.romi.mogumogu.testsupport.TestSecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RestaurantController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class RestaurantControllerTest {

    private static final String RESTAURANTS_PATH = "/restaurants";
    private static final String RESTAURANTS_MY_PATH = "/restaurants/my";
    private static final String RESTAURANTS_MY_RANDOM_PATH = "/restaurants/my/random";
    private static final String RESTAURANTS_MY_RANDOM_CLEAR_PATH = "/restaurants/my/random/clear";
    private static final String RESTAURANTS_MY_SELECTION_HISTORY_PATH = "/restaurants/my/selection-history";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String TRUNCATED_CREATE_RESTAURANT_JSON =
            "\u007b\"groupId\":1,\"categoryId\":2,\"restaurantName\":\"abc\"";
    private static final String TRUNCATED_UPDATE_RESTAURANT_JSON =
            "\u007b\"restaurantName\":\"abc\"";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantService restaurantService;

    @Nested
    class GetRestaurants {

        @Test
        void success_returnsRestaurantList() throws Exception {
            RestaurantResponse first = buildRestaurantResponse(1, 1, 10, "拉麵店");
            RestaurantResponse second = buildRestaurantResponse(2, 1, 11, "壽司店");
            stubGetRestaurants(matchesDefaultListQuery(),
                    buildRestaurantListResponse(List.of(first, second), 1, 20, 2L));

            performGetRestaurants()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.limit").value(20))
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].restaurantId").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("拉麵店"))
                    .andExpect(jsonPath("$.data[1].restaurantId").value(2))
                    .andExpect(jsonPath("$.data[1].restaurantName").value("壽司店"));

            verifyGetRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            stubGetRestaurants(matchesDefaultListQuery(),
                    buildRestaurantListResponse(List.of(), 1, 20, 0L));

            performGetRestaurants()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.limit").value(20))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verifyGetRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void withFilters_passesQueryParamsToService() throws Exception {
            RestaurantResponse filtered = buildRestaurantResponse(3, 2, 21, "牛排館");
            stubGetRestaurants(matchesFilteredListQuery(),
                    buildRestaurantListResponse(List.of(filtered), 1, 20, 1L));

            performGetRestaurants(Map.of(
                            "groupId", "2",
                            "categoryId", "21",
                            "isArchived", "false",
                            "search", "牛排"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantId").value(3))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("牛排館"));

            verifyGetRestaurantsCalled(matchesFilteredListQuery());
        }

        @Test
        void withOrderByAndSort_passesQueryParamsToService() throws Exception {
            RestaurantResponse sorted = buildRestaurantResponse(8, 1, 2, "燒肉店");
            stubGetRestaurants(matchesSortedListQuery(),
                    buildRestaurantListResponse(List.of(sorted), 1, 20, 1L));

            performGetRestaurants(Map.of(
                            "orderBy", "SELECTED_COUNT",
                            "sort", "DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantId").value(8))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("燒肉店"));

            verifyGetRestaurantsCalled(matchesSortedListQuery());
        }

        @Test
        void withPageAndLimit_passesQueryParamsToService() throws Exception {
            RestaurantResponse paged = buildRestaurantResponse(9, 1, 2, "火鍋店");
            stubGetRestaurants(matchesPagedListQuery(),
                    buildRestaurantListResponse(List.of(paged), 2, 5, 11L));

            performGetRestaurants(Map.of(
                            "page", "2",
                            "limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.limit").value(5))
                    .andExpect(jsonPath("$.total").value(11))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantId").value(9))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("火鍋店"));

            verifyGetRestaurantsCalled(matchesPagedListQuery());
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            stubGetRestaurantsThrows(matchesDefaultListQuery(), new RuntimeException("Database unavailable"));

            assertErrorResponse(performGetRestaurants(),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_PATH, "Database unavailable");

            verifyGetRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void serviceThrowsResponseStatusException_returns503() throws Exception {
            stubGetRestaurantsThrows(matchesDefaultListQuery(),
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable"));

            assertErrorResponse(performGetRestaurants(),
                    HttpStatus.SERVICE_UNAVAILABLE, RESTAURANTS_PATH, "Service temporarily unavailable");

            verifyGetRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void invalidOrderBy_returns400AndSkipsServiceCall() throws Exception {
            assertInvalidListQuery("orderBy", "INVALID_SORT_BY", "orderBy is invalid");
        }

        @Test
        void invalidSort_returns400AndSkipsServiceCall() throws Exception {
            assertInvalidListQuery("sort", "INVALID_SORT_ORDER", "sort is invalid");
        }

        @Test
        void invalidPage_returns400AndSkipsServiceCall() throws Exception {
            assertInvalidListQuery("page", "0", "page must be greater than or equal to the minimum value");
        }

        @Test
        void invalidLimit_returns400AndSkipsServiceCall() throws Exception {
            assertInvalidListQuery("limit", "0", "limit must be greater than or equal to the minimum value");
        }
    }

    @Nested
    class GetMyGroupRestaurants {

        @Test
        void success_returnsRestaurantList() throws Exception {
            RestaurantResponse first = buildRestaurantResponse(1, 1, 10, "拉麵店");
            stubGetMyGroupRestaurants(matchesDefaultListQuery(),
                    buildRestaurantListResponse(List.of(first), 1, 20, 1L));

            performGetMyGroupRestaurants()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("拉麵店"));

            verifyGetMyGroupRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            stubGetMyGroupRestaurants(matchesDefaultListQuery(),
                    buildRestaurantListResponse(List.of(), 1, 20, 0L));

            performGetMyGroupRestaurants()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verifyGetMyGroupRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void withFilters_passesQueryParamsToService() throws Exception {
            RestaurantResponse filtered = buildRestaurantResponse(3, 1, 21, "牛排館");
            stubGetMyGroupRestaurants(matchesMyGroupFilteredListQuery(),
                    buildRestaurantListResponse(List.of(filtered), 1, 20, 1L));

            performGetMyGroupRestaurants(Map.of(
                            "categoryId", "21",
                            "isArchived", "false",
                            "search", "牛排"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("牛排館"));

            verifyGetMyGroupRestaurantsCalled(matchesMyGroupFilteredListQuery());
        }

        @Test
        void withOrderByAndSort_passesQueryParamsToService() throws Exception {
            RestaurantResponse sorted = buildRestaurantResponse(8, 1, 2, "燒肉店");
            stubGetMyGroupRestaurants(matchesSortedListQuery(),
                    buildRestaurantListResponse(List.of(sorted), 1, 20, 1L));

            performGetMyGroupRestaurants(Map.of(
                            "orderBy", "SELECTED_COUNT",
                            "sort", "DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("燒肉店"));

            verifyGetMyGroupRestaurantsCalled(matchesSortedListQuery());
        }

        @Test
        void withPageAndLimit_passesQueryParamsToService() throws Exception {
            RestaurantResponse paged = buildRestaurantResponse(9, 1, 2, "火鍋店");
            stubGetMyGroupRestaurants(matchesPagedListQuery(),
                    buildRestaurantListResponse(List.of(paged), 2, 5, 11L));

            performGetMyGroupRestaurants(Map.of("page", "2", "limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.limit").value(5))
                    .andExpect(jsonPath("$.total").value(11));

            verifyGetMyGroupRestaurantsCalled(matchesPagedListQuery());
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantService.getMyGroupRestaurants(any(GetRestaurantQuery.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

            assertErrorResponse(performGetMyGroupRestaurants(),
                    HttpStatus.BAD_REQUEST, RESTAURANTS_MY_PATH, "該帳號未加入群組");

            verify(restaurantService).getMyGroupRestaurants(any(GetRestaurantQuery.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            stubGetMyGroupRestaurantsThrows(matchesDefaultListQuery(),
                    new RuntimeException("My group query failed"));

            assertErrorResponse(performGetMyGroupRestaurants(),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_MY_PATH, "My group query failed");

            verifyGetMyGroupRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void serviceThrowsResponseStatusException_returns503() throws Exception {
            stubGetMyGroupRestaurantsThrows(matchesDefaultListQuery(),
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable"));

            assertErrorResponse(performGetMyGroupRestaurants(),
                    HttpStatus.SERVICE_UNAVAILABLE, RESTAURANTS_MY_PATH, "Service temporarily unavailable");

            verifyGetMyGroupRestaurantsCalled(matchesDefaultListQuery());
        }

        @Test
        void invalidPage_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupRestaurants(Map.of("page", "0")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_PATH,
                    "page must be greater than or equal to the minimum value");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void invalidOrderBy_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupRestaurants(Map.of("orderBy", "INVALID_SORT_BY")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_PATH, "orderBy is invalid");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void invalidSort_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupRestaurants(Map.of("sort", "INVALID_SORT_ORDER")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_PATH, "sort is invalid");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void invalidLimit_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupRestaurants(Map.of("limit", "0")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_PATH,
                    "limit must be greater than or equal to the minimum value");

            verifyNoInteractions(restaurantService);
        }
    }

    @Nested
    class GetRandomMyGroupRestaurant {

        @Test
        void success_returnsRestaurant() throws Exception {
            RestaurantResponse randomRestaurant = buildRestaurantResponse(7, 1, 10, "隨機拉麵店");
            when(restaurantService.getRandomMyGroupRestaurant(null)).thenReturn(randomRestaurant);

            performGetRandomMyGroupRestaurant()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(7))
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.restaurantName").value("隨機拉麵店"));

            verify(restaurantService).getRandomMyGroupRestaurant(null);
        }

        @Test
        void withCategory_passesCategoryToService() throws Exception {
            RestaurantResponse randomRestaurant = buildRestaurantResponse(8, 1, 10, "分類隨機拉麵店");
            when(restaurantService.getRandomMyGroupRestaurant(10)).thenReturn(randomRestaurant);

            performGetRandomMyGroupRestaurant(Map.of("categoryId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(8))
                    .andExpect(jsonPath("$.restaurantName").value("分類隨機拉麵店"));

            verify(restaurantService).getRandomMyGroupRestaurant(10);
        }

        @Test
        void noRestaurant_returns404() throws Exception {
            when(restaurantService.getRandomMyGroupRestaurant(null))
                    .thenThrow(new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No available restaurants found for this filter"));

            assertErrorResponse(performGetRandomMyGroupRestaurant(),
                    HttpStatus.NOT_FOUND, RESTAURANTS_MY_RANDOM_PATH,
                    "No available restaurants found for this filter");

            verify(restaurantService).getRandomMyGroupRestaurant(null);
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantService.getRandomMyGroupRestaurant(null))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

            assertErrorResponse(performGetRandomMyGroupRestaurant(),
                    HttpStatus.BAD_REQUEST, RESTAURANTS_MY_RANDOM_PATH, "該帳號未加入群組");

            verify(restaurantService).getRandomMyGroupRestaurant(null);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.getRandomMyGroupRestaurant(null))
                    .thenThrow(new RuntimeException("Random draw failed"));

            assertErrorResponse(performGetRandomMyGroupRestaurant(),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_MY_RANDOM_PATH, "Random draw failed");

            verify(restaurantService).getRandomMyGroupRestaurant(null);
        }

        @Test
        void invalidCategoryId_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetRandomMyGroupRestaurant(Map.of("categoryId", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, RESTAURANTS_MY_RANDOM_PATH,
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantService);
        }
    }

    @Nested
    class ChooseMyGroupRestaurant {

        @Test
        void success_returns200WithUpdatedRestaurant() throws Exception {
            RestaurantResponse chosen = buildRestaurantResponse(7, 1, 10, "確認拉麵店");
            chosen.setSelectedCount(1);
            when(restaurantService.chooseMyGroupRestaurant(7)).thenReturn(chosen);

            performChooseMyGroupRestaurant(7)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(7))
                    .andExpect(jsonPath("$.selectedCount").value(1))
                    .andExpect(jsonPath("$.restaurantName").value("確認拉麵店"));

            verify(restaurantService).chooseMyGroupRestaurant(7);
        }

        @Test
        void notFound_returns404() throws Exception {
            when(restaurantService.chooseMyGroupRestaurant(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performChooseMyGroupRestaurant(999),
                    HttpStatus.NOT_FOUND, "/restaurants/my/choose/999", "Restaurant not found");

            verify(restaurantService).chooseMyGroupRestaurant(999);
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantService.chooseMyGroupRestaurant(7))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

            assertErrorResponse(performChooseMyGroupRestaurant(7),
                    HttpStatus.BAD_REQUEST, "/restaurants/my/choose/7", "該帳號未加入群組");

            verify(restaurantService).chooseMyGroupRestaurant(7);
        }

        @Test
        void forbidden_returns403() throws Exception {
            when(restaurantService.chooseMyGroupRestaurant(7))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Restaurant belongs to another group"));

            assertErrorResponse(performChooseMyGroupRestaurant(7),
                    HttpStatus.FORBIDDEN, "/restaurants/my/choose/7", "Restaurant belongs to another group");

            verify(restaurantService).chooseMyGroupRestaurant(7);
        }

        @Test
        void archived_returns410() throws Exception {
            when(restaurantService.chooseMyGroupRestaurant(7))
                    .thenThrow(new ResponseStatusException(HttpStatus.GONE, "Restaurant is archived"));

            assertErrorResponse(performChooseMyGroupRestaurant(7),
                    HttpStatus.GONE, "/restaurants/my/choose/7", "Restaurant is archived");

            verify(restaurantService).chooseMyGroupRestaurant(7);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(patch("/restaurants/my/choose/{id}", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/my/choose/bad-id",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.chooseMyGroupRestaurant(7))
                    .thenThrow(new RuntimeException("Choose failed unexpectedly"));

            assertErrorResponse(performChooseMyGroupRestaurant(7),
                    HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/my/choose/7", "Choose failed unexpectedly");

            verify(restaurantService).chooseMyGroupRestaurant(7);
        }
    }

    @Nested
    class GetMyGroupSelectionHistory {

        @Test
        void success_returnsSelectionHistoryList() throws Exception {
            Date now = new Date();
            SelectionHistoryResponse history = SelectionHistoryResponse.builder()
                    .historyId(1)
                    .restaurantId(22)
                    .restaurantName("乾乾拌拌")
                    .category("主食")
                    .selectedAt(now)
                    .build();
            stubGetMyGroupSelectionHistory(matchesDefaultHistoryQuery(),
                    buildSelectionHistoryListResponse(List.of(history), 1, 10, 35L));

            performGetMyGroupSelectionHistory()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.limit").value(10))
                    .andExpect(jsonPath("$.total").value(35))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].historyId").value(1))
                    .andExpect(jsonPath("$.data[0].restaurantName").value("乾乾拌拌"))
                    .andExpect(jsonPath("$.data[0].category").value("主食"));

            verifyGetMyGroupSelectionHistoryCalled(matchesDefaultHistoryQuery());
        }

        @Test
        void withPageAndLimit_passesQueryParamsToService() throws Exception {
            SelectionHistoryResponse paged = SelectionHistoryResponse.builder()
                    .historyId(2)
                    .restaurantId(8)
                    .restaurantName("壽司店")
                    .category("主食")
                    .selectedAt(new Date())
                    .build();
            stubGetMyGroupSelectionHistory(matchesPagedHistoryQuery(),
                    buildSelectionHistoryListResponse(List.of(paged), 2, 10, 35L));

            performGetMyGroupSelectionHistory(Map.of("page", "2", "limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.limit").value(10))
                    .andExpect(jsonPath("$.data.length()").value(1));

            verifyGetMyGroupSelectionHistoryCalled(matchesPagedHistoryQuery());
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            stubGetMyGroupSelectionHistory(matchesDefaultHistoryQuery(),
                    buildSelectionHistoryListResponse(List.of(), 1, 10, 0L));

            performGetMyGroupSelectionHistory()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.limit").value(10))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verifyGetMyGroupSelectionHistoryCalled(matchesDefaultHistoryQuery());
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantService.getMyGroupSelectionHistory(any(GetSelectionHistoryQuery.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

            assertErrorResponse(performGetMyGroupSelectionHistory(),
                    HttpStatus.BAD_REQUEST, RESTAURANTS_MY_SELECTION_HISTORY_PATH, "該帳號未加入群組");

            verify(restaurantService).getMyGroupSelectionHistory(any(GetSelectionHistoryQuery.class));
        }

        @Test
        void invalidPage_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupSelectionHistory(Map.of("page", "0")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_SELECTION_HISTORY_PATH,
                    "page must be greater than or equal to the minimum value");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void invalidLimit_returns400AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(performGetMyGroupSelectionHistory(Map.of("limit", "0")),
                    400, CODE_BAD_REQUEST, RESTAURANTS_MY_SELECTION_HISTORY_PATH,
                    "limit must be greater than or equal to the minimum value");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.getMyGroupSelectionHistory(any(GetSelectionHistoryQuery.class)))
                    .thenThrow(new RuntimeException("Selection history query failed"));

            assertErrorResponse(performGetMyGroupSelectionHistory(),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_MY_SELECTION_HISTORY_PATH,
                    "Selection history query failed");

            verify(restaurantService).getMyGroupSelectionHistory(any(GetSelectionHistoryQuery.class));
        }
    }

    @Nested
    class ClearMyGroupRandomPool {

        @Test
        void success_returns204() throws Exception {
            performClearMyGroupRandomPool()
                    .andExpect(status().isNoContent());

            verify(restaurantService).clearMyGroupRandomPool();
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            doThrow(new RuntimeException("Clear pool failed"))
                    .when(restaurantService).clearMyGroupRandomPool();

            assertErrorResponse(performClearMyGroupRandomPool(),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_MY_RANDOM_CLEAR_PATH, "Clear pool failed");

            verify(restaurantService).clearMyGroupRandomPool();
        }
    }

    @Nested
    class SingleRestaurantLookup {

        @Test
        void success_returnsRestaurant() throws Exception {
            RestaurantResponse restaurant = buildRestaurantResponse(5, 1, 2, "單筆查詢餐廳");
            when(restaurantService.getRestaurant(5)).thenReturn(restaurant);

            mockMvc.perform(get("/restaurants/{id}", 5))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(5))
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.categoryId").value(2))
                    .andExpect(jsonPath("$.restaurantName").value("單筆查詢餐廳"));

            verify(restaurantService).getRestaurant(5);
        }

        @Test
        void notFound_returns404() throws Exception {
            when(restaurantService.getRestaurant(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(mockMvc.perform(get("/restaurants/{id}", 999)),
                    HttpStatus.NOT_FOUND, "/restaurants/999", "Restaurant not found");

            verify(restaurantService).getRestaurant(999);
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantService.getRestaurant(5))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

            assertErrorResponse(mockMvc.perform(get("/restaurants/{id}", 5)),
                    HttpStatus.BAD_REQUEST, "/restaurants/5", "該帳號未加入群組");

            verify(restaurantService).getRestaurant(5);
        }

        @Test
        void forbidden_returns403() throws Exception {
            when(restaurantService.getRestaurant(5))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Restaurant belongs to another group"));

            assertErrorResponse(mockMvc.perform(get("/restaurants/{id}", 5)),
                    HttpStatus.FORBIDDEN, "/restaurants/5", "Restaurant belongs to another group");

            verify(restaurantService).getRestaurant(5);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.getRestaurant(7))
                    .thenThrow(new RuntimeException("Get restaurant failed"));

            assertErrorResponse(mockMvc.perform(get("/restaurants/{id}", 7)),
                    HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/7", "Get restaurant failed");

            verify(restaurantService).getRestaurant(7);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(get("/restaurants/{id}", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/bad-id",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantService);
        }
    }

    @Nested
    class GetRestaurantDishes {
        @Test
        void success_returnsDishList() throws Exception {
            DishResponse first = buildDishResponse(1, 100, 1, 120, "豚骨拉麵");
            DishResponse second = buildDishResponse(2, 100, 2, 90, "炸蝦天婦羅");
            when(restaurantService.getRestaurantDishes(100))
                    .thenReturn(DishListResponse.builder()
                            .data(List.of(first, second))
                            .total(2)
                            .build());

            performGetRestaurantDishes(100)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].dishId").value(1))
                    .andExpect(jsonPath("$.data[1].dishName").value("炸蝦天婦羅"));

            verify(restaurantService).getRestaurantDishes(100);
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            when(restaurantService.getRestaurantDishes(100))
                    .thenReturn(DishListResponse.builder()
                            .data(List.of())
                            .total(0)
                            .build());

            performGetRestaurantDishes(100)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(restaurantService).getRestaurantDishes(100);
        }

        @Test
        void restaurantNotFound_returns404() throws Exception {
            when(restaurantService.getRestaurantDishes(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performGetRestaurantDishes(999),
                    HttpStatus.NOT_FOUND, "/restaurants/999/dishes", "Restaurant not found");

            verify(restaurantService).getRestaurantDishes(999);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(get("/restaurants/{restaurantId}/dishes", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/bad-id/dishes",
                    "Failed to convert value of type");
            verifyNoInteractions(restaurantService);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.getRestaurantDishes(100))
                    .thenThrow(new RuntimeException("Get dishes failed"));

            assertErrorResponse(performGetRestaurantDishes(100),
                    HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/100/dishes", "Get dishes failed");

            verify(restaurantService).getRestaurantDishes(100);
        }
    }

    @Nested
    class CreateRestaurant {

        @Test
        void success_returns201AndCreatedRestaurant() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(1, 2, "和食天國");
            RestaurantResponse response = buildRestaurantResponse(100, 1, 2, "和食天國");

            when(restaurantService.createRestaurant(any(CreateRestaurantDto.class))).thenReturn(response);

            performPostRestaurants(request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.restaurantId").value(100))
                    .andExpect(jsonPath("$.groupId").value(1))
                    .andExpect(jsonPath("$.categoryId").value(2))
                    .andExpect(jsonPath("$.restaurantName").value("和食天國"))
                    .andExpect(jsonPath("$.isArchived").value(false));

            verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void validationFailed_returns400AndMessage() throws Exception {
            CreateRestaurantDto invalidRequest = CreateRestaurantDto.builder()
                    .groupId(-1)
                    .restaurantName("")
                    .build();

            assertBadRequestValidation(performPostRestaurants(invalidRequest),
                    "groupId must be greater than or equal to the minimum value",
                    "categoryId is required",
                    "restaurantName is required");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void restaurantNameTooLong_returns400AndSkipsServiceCall() throws Exception {
            assertBadRequestValidation(
                    performPostRestaurants(buildCreateRequest(1, 2, "x".repeat(65))),
                    "restaurantName size is out of allowed range");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void noteTooLong_returns400AndSkipsServiceCall() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(1, 2, "和食天國");
            request.setNote("n".repeat(513));

            assertBadRequestValidation(performPostRestaurants(request),
                    "note size is out of allowed range");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void imageUrlTooLong_returns400AndSkipsServiceCall() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(1, 2, "和食天國");
            request.setImageUrl("u".repeat(513));

            assertBadRequestValidation(performPostRestaurants(request),
                    "imageUrl size is out of allowed range");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void nullFieldsViaRawJson_returns400AndSkipsServiceCall() throws Exception {
            assertBadRequestValidation(performPostRestaurantsRaw(
                            "{\"groupId\":null,\"categoryId\":null,\"restaurantName\":null}"),
                    "groupId is required",
                    "categoryId is required",
                    "restaurantName is required");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void serviceThrowsGroupNotFound_returns400() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(999, 2, "群組不存在");
            when(restaurantService.createRestaurant(any(CreateRestaurantDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group not found"));

            assertErrorResponse(performPostRestaurants(request), HttpStatus.BAD_REQUEST, RESTAURANTS_PATH,
                    "Group not found");

            verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void serviceThrowsBadRequest_returns400ErrorPayload() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(1, 999, "不存在分類");
            when(restaurantService.createRestaurant(any(CreateRestaurantDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

            assertErrorResponse(performPostRestaurants(request), HttpStatus.BAD_REQUEST, RESTAURANTS_PATH,
                    "Category not found");

            verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500ErrorPayload() throws Exception {
            CreateRestaurantDto request = buildCreateRequest(1, 2, "系統錯誤餐廳");
            when(restaurantService.createRestaurant(any(CreateRestaurantDto.class)))
                    .thenThrow(new RuntimeException("Create failed unexpectedly"));

            assertErrorResponse(performPostRestaurants(request),
                    HttpStatus.INTERNAL_SERVER_ERROR, RESTAURANTS_PATH, "Create failed unexpectedly");

            verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void missingBody_returns500AndSkipsServiceCall() throws Exception {
            assertMissingJsonBodyError(RESTAURANTS_PATH);
        }

        @Test
        void invalidJson_returns500AndSkipsServiceCall() throws Exception {
            assertJsonParseError(RESTAURANTS_PATH,
                    performPostRestaurantsRaw(TRUNCATED_CREATE_RESTAURANT_JSON));
        }
    }

    @Nested
    class UpdateRestaurant {

        @Test
        void success_returns200WithUpdatedRestaurant() throws Exception {
            UpdateRestaurantDto request = buildUpdateRequest("新餐廳名稱", 20, 5);
            RestaurantResponse updated = buildRestaurantResponse(5, 1, 2, "新餐廳名稱");
            updated.setSelectedCount(20);
            updated.setDisplayOrderId(5);

            when(restaurantService.updateRestaurant(eq(5), any(UpdateRestaurantDto.class))).thenReturn(updated);

            performPatchRestaurant(5, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(5))
                    .andExpect(jsonPath("$.restaurantName").value("新餐廳名稱"))
                    .andExpect(jsonPath("$.selectedCount").value(20))
                    .andExpect(jsonPath("$.displayOrderId").value(5));

            verify(restaurantService).updateRestaurant(eq(5), any(UpdateRestaurantDto.class));
        }

        @Test
        void notFound_returns404() throws Exception {
            UpdateRestaurantDto request = buildUpdateRequest("更新失敗", null, null);
            when(restaurantService.updateRestaurant(eq(404), any(UpdateRestaurantDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performPatchRestaurant(404, request),
                    HttpStatus.NOT_FOUND, "/restaurants/404", "Restaurant not found");

            verify(restaurantService).updateRestaurant(eq(404), any(UpdateRestaurantDto.class));
        }

        @Test
        void serviceThrowsBadRequest_returns400() throws Exception {
            UpdateRestaurantDto request = buildUpdateRequest(null, null, 1);
            when(restaurantService.updateRestaurant(eq(12), any(UpdateRestaurantDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "displayOrderId already exists in this group"));

            assertErrorResponse(performPatchRestaurant(12, request),
                    HttpStatus.BAD_REQUEST, "/restaurants/12", "displayOrderId already exists in this group");

            verify(restaurantService).updateRestaurant(eq(12), any(UpdateRestaurantDto.class));
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            UpdateRestaurantDto request = buildUpdateRequest("更新時爆炸", null, null);
            when(restaurantService.updateRestaurant(eq(13), any(UpdateRestaurantDto.class)))
                    .thenThrow(new RuntimeException("Update failed unexpectedly"));

            assertErrorResponse(performPatchRestaurant(13, request),
                    HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/13", "Update failed unexpectedly");

            verify(restaurantService).updateRestaurant(eq(13), any(UpdateRestaurantDto.class));
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto request = buildUpdateRequest("不會送到 service", null, null);

            assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", "bad-id")
                            .contentType(CONTENT_TYPE_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(request)))),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/bad-id",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void validationFailed_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto invalidRequest = UpdateRestaurantDto.builder()
                    .displayOrderId(-1)
                    .restaurantName("x".repeat(65))
                    .build();

            assertBadRequestValidation(performPatchRestaurant(7, invalidRequest),
                    "displayOrderId must be greater than or equal to the minimum value",
                    "restaurantName size is out of allowed range");

            verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void noteTooLong_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto request = UpdateRestaurantDto.builder()
                    .note("n".repeat(513))
                    .build();

            assertBadRequestValidation(performPatchRestaurant(7, request),
                    "note size is out of allowed range");

            verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void imageUrlTooLong_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto request = UpdateRestaurantDto.builder()
                    .imageUrl("u".repeat(513))
                    .build();

            assertBadRequestValidation(performPatchRestaurant(7, request),
                    "imageUrl size is out of allowed range");

            verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void negativeSelectedCount_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto request = UpdateRestaurantDto.builder()
                    .selectedCount(-1)
                    .build();

            assertBadRequestValidation(performPatchRestaurant(7, request),
                    "selectedCount must be greater than or equal to the minimum value");

            verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void negativeCategoryId_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantDto request = UpdateRestaurantDto.builder()
                    .categoryId(-1)
                    .build();

            assertBadRequestValidation(performPatchRestaurant(7, request),
                    "categoryId must be greater than or equal to the minimum value");

            verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void serviceThrowsCategoryNotFound_returns400() throws Exception {
            UpdateRestaurantDto request = UpdateRestaurantDto.builder().categoryId(999).build();
            when(restaurantService.updateRestaurant(eq(12), any(UpdateRestaurantDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

            assertErrorResponse(performPatchRestaurant(12, request),
                    HttpStatus.BAD_REQUEST, "/restaurants/12", "Category not found");

            verify(restaurantService).updateRestaurant(eq(12), any(UpdateRestaurantDto.class));
        }

        @Test
        void missingBody_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", 1)
                            .contentType(CONTENT_TYPE_JSON)),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/1",
                    "Required request body is missing");

            verifyNoInteractions(restaurantService);
        }

        @Test
        void invalidJson_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", 1)
                            .contentType(CONTENT_TYPE_JSON)
                            .content(TRUNCATED_UPDATE_RESTAURANT_JSON)),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/1", "JSON parse error");

            verifyNoInteractions(restaurantService);
        }
    }

    @Nested
    class DeleteRestaurant {

        @Test
        void success_returns200AndArchivedRestaurant() throws Exception {
            RestaurantResponse deleted = buildRestaurantResponse(10, 1, 2, "刪除前餐廳");
            deleted.setIsArchived(true);
            when(restaurantService.deleteRestaurant(10)).thenReturn(deleted);

            performDeleteRestaurant(10)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(10))
                    .andExpect(jsonPath("$.isArchived").value(true));

            verify(restaurantService).deleteRestaurant(10);
        }

        @Test
        void notFound_returns404() throws Exception {
            when(restaurantService.deleteRestaurant(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performDeleteRestaurant(999),
                    HttpStatus.NOT_FOUND, "/restaurants/999", "Restaurant not found");

            verify(restaurantService).deleteRestaurant(999);
        }

        @Test
        void serviceThrowsBadRequest_returns400() throws Exception {
            when(restaurantService.deleteRestaurant(77))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid restaurant state"));

            assertErrorResponse(performDeleteRestaurant(77),
                    HttpStatus.BAD_REQUEST, "/restaurants/77", "Invalid restaurant state");

            verify(restaurantService).deleteRestaurant(77);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantService.deleteRestaurant(88))
                    .thenThrow(new RuntimeException("Delete failed unexpectedly"));

            assertErrorResponse(performDeleteRestaurant(88),
                    HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/88", "Delete failed unexpectedly");

            verify(restaurantService).deleteRestaurant(88);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(delete("/restaurants/{id}", "abc")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/restaurants/abc",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantService);
        }
    }

    private void stubGetRestaurants(
            java.util.function.Predicate<GetRestaurantQuery> matcher,
            RestaurantListResponse<RestaurantResponse> response) {
        when(restaurantService.getRestaurants(argThat(matcher::test))).thenReturn(response);
    }

    private void stubGetRestaurantsThrows(
            java.util.function.Predicate<GetRestaurantQuery> matcher,
            Throwable throwable) {
        when(restaurantService.getRestaurants(argThat(matcher::test))).thenThrow(throwable);
    }

    private void stubGetMyGroupRestaurants(
            java.util.function.Predicate<GetRestaurantQuery> matcher,
            RestaurantListResponse<RestaurantResponse> response) {
        when(restaurantService.getMyGroupRestaurants(argThat(matcher::test))).thenReturn(response);
    }

    private void stubGetMyGroupRestaurantsThrows(
            java.util.function.Predicate<GetRestaurantQuery> matcher,
            Throwable throwable) {
        when(restaurantService.getMyGroupRestaurants(argThat(matcher::test))).thenThrow(throwable);
    }

    private void verifyGetRestaurantsCalled(java.util.function.Predicate<GetRestaurantQuery> matcher) {
        verify(restaurantService).getRestaurants(argThat(matcher::test));
    }

    private void verifyGetMyGroupRestaurantsCalled(java.util.function.Predicate<GetRestaurantQuery> matcher) {
        verify(restaurantService).getMyGroupRestaurants(argThat(matcher::test));
    }

    private void assertInvalidListQuery(String paramName, String paramValue, String messagePart) throws Exception {
        assertErrorResponseContains(performGetRestaurants(Map.of(paramName, paramValue)),
                400, CODE_BAD_REQUEST, RESTAURANTS_PATH, messagePart);

        verifyNoInteractions(restaurantService);
    }

    private void assertBadRequestValidation(ResultActions resultActions, String... messageParts) throws Exception {
        String responseJson = resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(CODE_BAD_REQUEST))
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (String part : messageParts) {
            assertMessageContains(responseJson, part);
        }
    }

    private void assertMissingJsonBodyError(String path) throws Exception {
        final String safePath = Objects.requireNonNull(path, "path");
        assertErrorResponseContains(
                mockMvc.perform(post(safePath).contentType(CONTENT_TYPE_JSON)),
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                safePath,
                "Required request body is missing");
        verifyNoInteractions(restaurantService);
    }

    private void assertJsonParseError(String path, ResultActions truncatedJsonPost) throws Exception {
        assertErrorResponseContains(
                truncatedJsonPost,
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                path,
                "JSON parse error");
        verifyNoInteractions(restaurantService);
    }

    private RestaurantResponse buildRestaurantResponse(Integer restaurantId, Integer groupId, Integer categoryId,
            String name) {
        Date now = new Date();
        return RestaurantResponse.builder()
                .restaurantId(restaurantId)
                .groupId(groupId)
                .categoryId(categoryId)
                .displayOrderId(1)
                .selectedCount(0)
                .restaurantName(name)
                .note("測試備註")
                .imageUrl("https://example.com/image.jpg")
                .isArchived(false)
                .lastSelectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private CreateRestaurantDto buildCreateRequest(Integer groupId, Integer categoryId, String restaurantName) {
        return CreateRestaurantDto.builder()
                .groupId(groupId)
                .categoryId(categoryId)
                .restaurantName(restaurantName)
                .note("可訂位")
                .imageUrl("https://example.com/restaurant.jpg")
                .build();
    }

    private UpdateRestaurantDto buildUpdateRequest(String restaurantName, Integer selectedCount,
            Integer displayOrderId) {
        return UpdateRestaurantDto.builder()
                .restaurantName(restaurantName)
                .selectedCount(selectedCount)
                .displayOrderId(displayOrderId)
                .build();
    }

    private RestaurantListResponse<RestaurantResponse> buildRestaurantListResponse(List<RestaurantResponse> data,
            Integer page, Integer limit, Long total) {
        return RestaurantListResponse.of(data, page, limit, total);
    }

    private ResultActions performPostRestaurants(Object request) throws Exception {
        return mockMvc.perform(post(RESTAURANTS_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performPostRestaurantsRaw(String payload) throws Exception {
        return mockMvc.perform(post(RESTAURANTS_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(payload)));
    }

    private ResultActions performPatchRestaurant(Integer id, Object request) throws Exception {
        return mockMvc.perform(patch("/restaurants/{id}", id)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performDeleteRestaurant(Integer id) throws Exception {
        return mockMvc.perform(delete("/restaurants/{id}", id));
    }

    private ResultActions performGetRestaurants() throws Exception {
        return mockMvc.perform(get(RESTAURANTS_PATH));
    }

    private ResultActions performGetRestaurants(Map<String, String> queryParams) throws Exception {
        var requestBuilder = get(RESTAURANTS_PATH);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performGetMyGroupRestaurants() throws Exception {
        return mockMvc.perform(get(RESTAURANTS_MY_PATH));
    }

    private ResultActions performGetMyGroupRestaurants(Map<String, String> queryParams) throws Exception {
        var requestBuilder = get(RESTAURANTS_MY_PATH);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performGetRandomMyGroupRestaurant() throws Exception {
        return mockMvc.perform(get(RESTAURANTS_MY_RANDOM_PATH));
    }

    private ResultActions performGetRandomMyGroupRestaurant(Map<String, String> queryParams) throws Exception {
        var requestBuilder = get(RESTAURANTS_MY_RANDOM_PATH);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performChooseMyGroupRestaurant(Integer id) throws Exception {
        return mockMvc.perform(patch("/restaurants/my/choose/{id}", id));
    }

    private ResultActions performClearMyGroupRandomPool() throws Exception {
        return mockMvc.perform(post(RESTAURANTS_MY_RANDOM_CLEAR_PATH));
    }

    private ResultActions performGetMyGroupSelectionHistory() throws Exception {
        return mockMvc.perform(get(RESTAURANTS_MY_SELECTION_HISTORY_PATH));
    }

    private ResultActions performGetMyGroupSelectionHistory(Map<String, String> queryParams) throws Exception {
        var requestBuilder = get(RESTAURANTS_MY_SELECTION_HISTORY_PATH);
        queryParams.forEach(requestBuilder::param);
        return mockMvc.perform(requestBuilder);
    }

    private void stubGetMyGroupSelectionHistory(
            java.util.function.Predicate<GetSelectionHistoryQuery> matcher,
            RestaurantListResponse<SelectionHistoryResponse> response) {
        when(restaurantService.getMyGroupSelectionHistory(argThat(matcher::test))).thenReturn(response);
    }

    private void verifyGetMyGroupSelectionHistoryCalled(
            java.util.function.Predicate<GetSelectionHistoryQuery> matcher) {
        verify(restaurantService).getMyGroupSelectionHistory(argThat(matcher::test));
    }

    private RestaurantListResponse<SelectionHistoryResponse> buildSelectionHistoryListResponse(
            List<SelectionHistoryResponse> data, Integer page, Integer limit, Long total) {
        return RestaurantListResponse.of(data, page, limit, total);
    }

    private static java.util.function.Predicate<GetSelectionHistoryQuery> matchesDefaultHistoryQuery() {
        return p -> Objects.equals(p.getPage(), 1) && Objects.equals(p.getLimit(), 10);
    }

    private static java.util.function.Predicate<GetSelectionHistoryQuery> matchesPagedHistoryQuery() {
        return p -> Objects.equals(p.getPage(), 2) && Objects.equals(p.getLimit(), 10);
    }

    private void assertMessageContains(String responseJson, String messagePart) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode messageNode = rootNode.get("message");
        assertNotNull(messageNode, "message field should exist");
        assertTrue(messageNode.asText().contains(messagePart),
                "message should contain: " + messagePart + " but was: " + messageNode.asText());
    }

    private static java.util.function.Predicate<GetRestaurantQuery> matchesDefaultListQuery() {
        return p -> matchesListQuery(
                p,
                null,
                null,
                null,
                null,
                RestaurantSort.SortBy.RESTAURANT_ID,
                RestaurantSort.SortOrder.ASC,
                1,
                20);
    }

    private static java.util.function.Predicate<GetRestaurantQuery> matchesFilteredListQuery() {
        return p -> matchesListQuery(
                p,
                2,
                21,
                false,
                "牛排",
                RestaurantSort.SortBy.RESTAURANT_ID,
                RestaurantSort.SortOrder.ASC,
                1,
                20);
    }

    private static java.util.function.Predicate<GetRestaurantQuery> matchesMyGroupFilteredListQuery() {
        return p -> matchesListQuery(
                p,
                null,
                21,
                false,
                "牛排",
                RestaurantSort.SortBy.RESTAURANT_ID,
                RestaurantSort.SortOrder.ASC,
                1,
                20);
    }

    private static java.util.function.Predicate<GetRestaurantQuery> matchesSortedListQuery() {
        return p -> matchesListQuery(
                p,
                null,
                null,
                null,
                null,
                RestaurantSort.SortBy.SELECTED_COUNT,
                RestaurantSort.SortOrder.DESC,
                1,
                20);
    }

    private static java.util.function.Predicate<GetRestaurantQuery> matchesPagedListQuery() {
        return p -> matchesListQuery(
                p,
                null,
                null,
                null,
                null,
                RestaurantSort.SortBy.RESTAURANT_ID,
                RestaurantSort.SortOrder.ASC,
                2,
                5);
    }

    private ResultActions performGetRestaurantDishes(Integer restaurantId) throws Exception {
        return mockMvc.perform(get("/restaurants/{restaurantId}/dishes", restaurantId));
    }

    private DishResponse buildDishResponse(Integer dishId, Integer restaurantId, Integer displayOrderId,
            Integer price, String dishName) {
        return DishResponse.builder()
                .dishId(dishId)
                .restaurantId(restaurantId)
                .displayOrderId(displayOrderId)
                .price(price)
                .dishName(dishName)
                .build();
    }

    private static boolean matchesListQuery(
            GetRestaurantQuery p,
            Integer groupId,
            Integer categoryId,
            Boolean isArchived,
            String search,
            RestaurantSort.SortBy orderBy,
            RestaurantSort.SortOrder sort,
            Integer page,
            Integer limit) {
        return Objects.equals(p.getGroupId(), groupId)
                && Objects.equals(p.getCategoryId(), categoryId)
                && Objects.equals(p.getIsArchived(), isArchived)
                && Objects.equals(p.getSearch(), search)
                && p.getOrderBy() == orderBy
                && p.getSort() == sort
                && Objects.equals(p.getPage(), page)
                && Objects.equals(p.getLimit(), limit);
    }
}
