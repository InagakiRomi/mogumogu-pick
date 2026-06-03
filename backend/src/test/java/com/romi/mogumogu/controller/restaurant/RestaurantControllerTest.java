package com.romi.mogumogu.controller.restaurant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
import com.romi.mogumogu.dto.UpdateRestaurantDto;
import com.romi.mogumogu.enums.RestaurantSort;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import com.romi.mogumogu.testsupport.TestSecurityConfig;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
@Import(TestSecurityConfig.class)
class RestaurantControllerTest {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private RestaurantService restaurantService;

        @Test
        void testCreateRestaurant_success_returns201AndCreatedRestaurant() throws Exception {
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
        void testCreateRestaurant_validationFailed_returns400AndMessage() throws Exception {
                CreateRestaurantDto invalidRequest = CreateRestaurantDto.builder()
                                .groupId(-1)
                                .restaurantName("")
                                .build();

                String responseJson = performPostRestaurants(invalidRequest)
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.result").value("error"))
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                                .andExpect(jsonPath("$.path").value("/restaurants"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();
                assertMessageContains(responseJson,
                                "groupId must be greater than or equal to the minimum value");
                assertMessageContains(responseJson, "categoryId is required");
                assertMessageContains(responseJson, "restaurantName is required");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testCreateRestaurant_serviceThrowsBadRequest_returns400ErrorPayload() throws Exception {
                CreateRestaurantDto request = buildCreateRequest(1, 999, "不存在分類");
                when(restaurantService.createRestaurant(any(CreateRestaurantDto.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

                assertErrorResponse(performPostRestaurants(request), HttpStatus.BAD_REQUEST, "/restaurants",
                                "Category not found");

                verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void testCreateRestaurant_serviceThrowsUnexpectedException_returns500ErrorPayload() throws Exception {
                CreateRestaurantDto request = buildCreateRequest(1, 2, "系統錯誤餐廳");
                when(restaurantService.createRestaurant(any(CreateRestaurantDto.class)))
                                .thenThrow(new RuntimeException("Create failed unexpectedly"));

                assertErrorResponse(performPostRestaurants(request),
                                HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants", "Create failed unexpectedly");

                verify(restaurantService).createRestaurant(any(CreateRestaurantDto.class));
        }

        @Test
        void testCreateRestaurant_missingBody_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(performPostRestaurantsWithoutBody(),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants", "Required request body is missing");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testCreateRestaurant_invalidJson_returns400AndSkipsServiceCall() throws Exception {
                String invalidJson = "{\"groupId\":1,\"categoryId\":2,\"restaurantName\":\"abc\"";

                assertErrorResponseContains(performPostRestaurantsRaw(invalidJson),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants", "JSON parse error");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testDeleteRestaurant_success_returns200AndArchivedRestaurant() throws Exception {
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
        void testDeleteRestaurant_notFound_returns404() throws Exception {
                when(restaurantService.deleteRestaurant(999))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

                assertErrorResponse(performDeleteRestaurant(999),
                                HttpStatus.NOT_FOUND, "/restaurants/999", "Restaurant not found");

                verify(restaurantService).deleteRestaurant(999);
        }

        @Test
        void testDeleteRestaurant_serviceThrowsBadRequest_returns400() throws Exception {
                when(restaurantService.deleteRestaurant(77))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Invalid restaurant state"));

                assertErrorResponse(performDeleteRestaurant(77),
                                HttpStatus.BAD_REQUEST, "/restaurants/77", "Invalid restaurant state");

                verify(restaurantService).deleteRestaurant(77);
        }

        @Test
        void testDeleteRestaurant_serviceThrowsUnexpectedException_returns500() throws Exception {
                when(restaurantService.deleteRestaurant(88))
                                .thenThrow(new RuntimeException("Delete failed unexpectedly"));

                assertErrorResponse(performDeleteRestaurant(88),
                                HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/88", "Delete failed unexpectedly");

                verify(restaurantService).deleteRestaurant(88);
        }

        @Test
        void testDeleteRestaurant_invalidPathVariable_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(mockMvc.perform(delete("/restaurants/{id}", "abc")),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants/abc", "Failed to convert value of type");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testGetRestaurants_success_returnsRestaurantList() throws Exception {
                RestaurantResponse first = buildRestaurantResponse(1, 1, 10, "拉麵店");
                RestaurantResponse second = buildRestaurantResponse(2, 1, 11, "壽司店");
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(first, second), 1, 20, 2L));

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

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery));
        }

        @Test
        void testGetRestaurants_success_returnsEmptyList() throws Exception {
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(), 1, 20, 0L));

                performGetRestaurants()
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.limit").value(20))
                                .andExpect(jsonPath("$.total").value(0))
                                .andExpect(jsonPath("$.data.length()").value(0));

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery));
        }

        @Test
        void testGetRestaurants_serviceThrowsUnexpectedException_returns500() throws Exception {
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery)))
                                .thenThrow(new RuntimeException("Database unavailable"));

                assertErrorResponse(performGetRestaurants(),
                                HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants", "Database unavailable");

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery));
        }

        @Test
        void testGetRestaurants_serviceThrowsResponseStatusException_returns503() throws Exception {
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery)))
                                .thenThrow(
                                                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                                                "Service temporarily unavailable"));

                assertErrorResponse(performGetRestaurants(),
                                HttpStatus.SERVICE_UNAVAILABLE, "/restaurants", "Service temporarily unavailable");

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery));
        }

        @Test
        void testGetRestaurants_withFilters_passesQueryParamsToService() throws Exception {
                RestaurantResponse filtered = buildRestaurantResponse(3, 2, 21, "牛排館");
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesFilteredListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(filtered), 1, 20, 1L));

                performGetRestaurants(Map.of(
                                "groupId", "2",
                                "categoryId", "21",
                                "isArchived", "false",
                                "search", "牛排"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[0].restaurantId").value(3))
                                .andExpect(jsonPath("$.data[0].restaurantName").value("牛排館"));

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesFilteredListQuery));
        }

        @Test
        void testGetRestaurants_withOrderByAndSort_passesQueryParamsToService() throws Exception {
                RestaurantResponse sorted = buildRestaurantResponse(8, 1, 2, "燒肉店");
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesSortedListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(sorted), 1, 20, 1L));

                performGetRestaurants(Map.of(
                                "orderBy", "SELECTED_COUNT",
                                "sort", "DESC"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[0].restaurantId").value(8))
                                .andExpect(jsonPath("$.data[0].restaurantName").value("燒肉店"));

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesSortedListQuery));
        }

        @Test
        void testGetRestaurants_withPageAndLimit_passesQueryParamsToService() throws Exception {
                RestaurantResponse paged = buildRestaurantResponse(9, 1, 2, "火鍋店");
                when(restaurantService.getRestaurants(argThat(RestaurantControllerTest::matchesPagedListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(paged), 2, 5, 11L));

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

                verify(restaurantService).getRestaurants(argThat(RestaurantControllerTest::matchesPagedListQuery));
        }

        @Test
        void testGetRestaurants_invalidOrderBy_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(performGetRestaurants(Map.of("orderBy", "INVALID_SORT_BY")),
                                400, "BAD_REQUEST", "/restaurants", "orderBy is invalid");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testGetRestaurants_invalidSort_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(performGetRestaurants(Map.of("sort", "INVALID_SORT_ORDER")),
                                400, "BAD_REQUEST", "/restaurants", "sort is invalid");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testGetRestaurants_invalidPage_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(performGetRestaurants(Map.of("page", "0")),
                                400, "BAD_REQUEST", "/restaurants",
                                "page must be greater than or equal to the minimum value");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testGetRestaurants_invalidLimit_returns400AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(performGetRestaurants(Map.of("limit", "0")),
                                400, "BAD_REQUEST", "/restaurants",
                                "limit must be greater than or equal to the minimum value");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testGetMyGroupRestaurants_success_returnsRestaurantList() throws Exception {
                RestaurantResponse first = buildRestaurantResponse(1, 1, 10, "拉麵店");
                when(restaurantService.getMyGroupRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery)))
                                .thenReturn(buildRestaurantListResponse(List.of(first), 1, 20, 1L));

                performGetMyGroupRestaurants()
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[0].restaurantName").value("拉麵店"));

                verify(restaurantService).getMyGroupRestaurants(argThat(RestaurantControllerTest::matchesDefaultListQuery));
        }

        @Test
        void testGetMyGroupRestaurants_notInGroup_returns400() throws Exception {
                when(restaurantService.getMyGroupRestaurants(any(GetRestaurantQuery.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "該帳號未加入群組"));

                assertErrorResponse(performGetMyGroupRestaurants(),
                                HttpStatus.BAD_REQUEST, "/restaurants/my", "該帳號未加入群組");

                verify(restaurantService).getMyGroupRestaurants(any(GetRestaurantQuery.class));
        }

        @Test
        void testUpdateRestaurant_success_returns200WithUpdatedRestaurant() throws Exception {
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
        void testUpdateRestaurant_notFound_returns404() throws Exception {
                UpdateRestaurantDto request = buildUpdateRequest("更新失敗", null, null);
                when(restaurantService.updateRestaurant(eq(404), any(UpdateRestaurantDto.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

                assertErrorResponse(performPatchRestaurant(404, request),
                                HttpStatus.NOT_FOUND, "/restaurants/404", "Restaurant not found");

                verify(restaurantService).updateRestaurant(eq(404), any(UpdateRestaurantDto.class));
        }

        @Test
        void testUpdateRestaurant_serviceThrowsBadRequest_returns400() throws Exception {
                UpdateRestaurantDto request = buildUpdateRequest(null, null, 1);
                when(restaurantService.updateRestaurant(eq(12), any(UpdateRestaurantDto.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "displayOrderId already exists in this group"));

                assertErrorResponse(performPatchRestaurant(12, request),
                                HttpStatus.BAD_REQUEST, "/restaurants/12", "displayOrderId already exists in this group");

                verify(restaurantService).updateRestaurant(eq(12), any(UpdateRestaurantDto.class));
        }

        @Test
        void testUpdateRestaurant_serviceThrowsUnexpectedException_returns500() throws Exception {
                UpdateRestaurantDto request = buildUpdateRequest("更新時爆炸", null, null);
                when(restaurantService.updateRestaurant(eq(13), any(UpdateRestaurantDto.class)))
                                .thenThrow(new RuntimeException("Update failed unexpectedly"));

                assertErrorResponse(performPatchRestaurant(13, request),
                                HttpStatus.INTERNAL_SERVER_ERROR, "/restaurants/13", "Update failed unexpectedly");

                verify(restaurantService).updateRestaurant(eq(13), any(UpdateRestaurantDto.class));
        }

        @Test
        void testUpdateRestaurant_invalidPathVariable_returns400AndSkipsServiceCall() throws Exception {
                UpdateRestaurantDto request = buildUpdateRequest("不會送到 service", null, null);

                assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", "bad-id")
                                .contentType("application/json")
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request)))),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants/bad-id", "Failed to convert value of type");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testUpdateRestaurant_validationFailed_returns400AndSkipsServiceCall() throws Exception {
                UpdateRestaurantDto invalidRequest = UpdateRestaurantDto.builder()
                                .displayOrderId(-1)
                                .restaurantName("x".repeat(65))
                                .build();

                String responseJson = performPatchRestaurant(7, invalidRequest)
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statusCode").value(400))
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();
                assertMessageContains(responseJson,
                                "displayOrderId must be greater than or equal to the minimum value");
                assertMessageContains(responseJson,
                                "restaurantName size is out of allowed range");

                verify(restaurantService, never()).updateRestaurant(eq(7), any(UpdateRestaurantDto.class));
        }

        @Test
        void testUpdateRestaurant_missingBody_returns500AndSkipsServiceCall() throws Exception {
                assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", 1)
                                .contentType("application/json")),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants/1", "Required request body is missing");

                verifyNoInteractions(restaurantService);
        }

        @Test
        void testUpdateRestaurant_invalidJson_returns500AndSkipsServiceCall() throws Exception {
                String invalidJson = "{\"restaurantName\":\"abc\""; // 缺結尾 }

                assertErrorResponseContains(mockMvc.perform(patch("/restaurants/{id}", 1)
                                .contentType("application/json")
                                .content(Objects.requireNonNull(invalidJson))),
                                500, "INTERNAL_SERVER_ERROR", "/restaurants/1", "JSON parse error");

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

        private RestaurantListResponse buildRestaurantListResponse(List<RestaurantResponse> data, Integer page,
                        Integer limit, Long total) {
                return RestaurantListResponse.builder()
                                .data(data)
                                .page(page)
                                .limit(limit)
                                .total(total)
                                .build();
        }

        private ResultActions performPostRestaurants(Object request) throws Exception {
                return mockMvc.perform(post("/restaurants")
                                .contentType("application/json")
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
        }

        private ResultActions performPostRestaurantsWithoutBody() throws Exception {
                return mockMvc.perform(post("/restaurants")
                                .contentType("application/json"));
        }

        private ResultActions performPostRestaurantsRaw(String payload) throws Exception {
                return mockMvc.perform(post("/restaurants")
                                .contentType("application/json")
                                .content(Objects.requireNonNull(payload)));
        }

        private ResultActions performPatchRestaurant(Integer id, Object request) throws Exception {
                return mockMvc.perform(patch("/restaurants/{id}", id)
                                .contentType("application/json")
                                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
        }

        private ResultActions performDeleteRestaurant(Integer id) throws Exception {
                return mockMvc.perform(delete("/restaurants/{id}", id));
        }

        private ResultActions performGetRestaurants() throws Exception {
                return mockMvc.perform(get("/restaurants"));
        }

        private ResultActions performGetRestaurants(Map<String, String> queryParams) throws Exception {
                var requestBuilder = get("/restaurants");
                queryParams.forEach(requestBuilder::param);
                return mockMvc.perform(requestBuilder);
        }

        private ResultActions performGetMyGroupRestaurants() throws Exception {
                return mockMvc.perform(get("/restaurants/my"));
        }

        private void assertMessageContains(String responseJson, String messagePart) throws Exception {
                JsonNode rootNode = objectMapper.readTree(responseJson);
                JsonNode messageNode = rootNode.get("message");
                assertNotNull(messageNode, "message field should exist");
                assertTrue(messageNode.asText().contains(messagePart), "message should contain: " + messagePart);
        }

        private static boolean matchesDefaultListQuery(GetRestaurantQuery p) {
                return matchesListQuery(
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

        private static boolean matchesFilteredListQuery(GetRestaurantQuery p) {
                return matchesListQuery(
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

        private static boolean matchesSortedListQuery(GetRestaurantQuery p) {
                return matchesListQuery(
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

        private static boolean matchesPagedListQuery(GetRestaurantQuery p) {
                return matchesListQuery(
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
