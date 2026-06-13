package com.romi.mogumogu.controller.restaurant;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.RestaurantCategoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantCategoryDto;
import com.romi.mogumogu.dto.UpdateRestaurantCategoryDto;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.restaurant.RestaurantCategoryService;
import com.romi.mogumogu.testsupport.TestSecurityConfig;

@WebMvcTest(controllers = RestaurantCategoryController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class RestaurantCategoryControllerTest {

    private static final String CATEGORIES_PATH = "/restaurant-categories";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String TRUNCATED_CREATE_CATEGORY_JSON = "{\"categoryName\":\"abc\"";
    private static final String TRUNCATED_UPDATE_CATEGORY_JSON = "{\"categoryName\":\"abc\"";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantCategoryService restaurantCategoryService;

    @Nested
    class GetMyGroupCategories {

        @Test
        void success_returnsCategoryList() throws Exception {
            when(restaurantCategoryService.getMyGroupCategories()).thenReturn(List.of(
                    buildCategoryResponse(1, "主食", 1, 5),
                    buildCategoryResponse(2, "輕食", 2, 0)));

            mockMvc.perform(get(CATEGORIES_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].categoryId").value(1))
                    .andExpect(jsonPath("$[0].categoryName").value("主食"))
                    .andExpect(jsonPath("$[0].restaurantCount").value(5))
                    .andExpect(jsonPath("$[1].categoryName").value("輕食"))
                    .andExpect(jsonPath("$[1].restaurantCount").value(0));

            verify(restaurantCategoryService).getMyGroupCategories();
        }

        @Test
        void success_returnsEmptyList() throws Exception {
            when(restaurantCategoryService.getMyGroupCategories()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(CATEGORIES_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(restaurantCategoryService).getMyGroupCategories();
        }

        @Test
        void notInGroup_returns400() throws Exception {
            when(restaurantCategoryService.getMyGroupCategories())
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(mockMvc.perform(get(CATEGORIES_PATH)),
                    HttpStatus.BAD_REQUEST, CATEGORIES_PATH, "User is not in a group");

            verify(restaurantCategoryService).getMyGroupCategories();
        }

        @Test
        void userNotFound_returns401() throws Exception {
            when(restaurantCategoryService.getMyGroupCategories())
                    .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            assertErrorResponse(mockMvc.perform(get(CATEGORIES_PATH)),
                    HttpStatus.UNAUTHORIZED, CATEGORIES_PATH, "User not found");

            verify(restaurantCategoryService).getMyGroupCategories();
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            when(restaurantCategoryService.getMyGroupCategories())
                    .thenThrow(new RuntimeException("Category list query failed"));

            assertErrorResponse(mockMvc.perform(get(CATEGORIES_PATH)),
                    HttpStatus.INTERNAL_SERVER_ERROR, CATEGORIES_PATH, "Category list query failed");

            verify(restaurantCategoryService).getMyGroupCategories();
        }
    }

    @Nested
    class CreateCategory {

        @Test
        void success_returns201() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("甜點")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenReturn(buildCategoryResponse(4, "甜點", 4));

            performCreateCategory(request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categoryId").value(4))
                    .andExpect(jsonPath("$.categoryName").value("甜點"));

            verify(restaurantCategoryService).createCategory(any(CreateRestaurantCategoryDto.class));
        }

        @Test
        void validationFailed_returns400AndSkipsServiceCall() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("")
                    .build();

            assertErrorResponseContains(performCreateCategory(request), 400, CODE_BAD_REQUEST, CATEGORIES_PATH,
                    "categoryName is required");

            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void blankCategoryName_returns400AndSkipsServiceCall() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("  \t\n")
                    .build();

            assertBadRequestValidation(performCreateCategory(request), CATEGORIES_PATH, "categoryName is required");
            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void categoryNameTooLong_returns400AndSkipsServiceCall() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("x".repeat(33))
                    .build();

            assertBadRequestValidation(performCreateCategory(request), CATEGORIES_PATH,
                    "categoryName size is out of allowed range");
            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void nullCategoryNameViaRawJson_returns400AndSkipsServiceCall() throws Exception {
            assertBadRequestValidation(performCreateCategoryRaw("{\"categoryName\":null}"),
                    CATEGORIES_PATH, "categoryName is required");
            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void missingBody_returns500AndSkipsServiceCall() throws Exception {
            assertMissingJsonBodyError(CATEGORIES_PATH);
        }

        @Test
        void invalidJson_returns500AndSkipsServiceCall() throws Exception {
            assertJsonParseError(CATEGORIES_PATH, performCreateCategoryRaw(TRUNCATED_CREATE_CATEGORY_JSON));
        }

        @Test
        void duplicateName_returns409() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("主食")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists"));

            assertErrorResponse(performCreateCategory(request), HttpStatus.CONFLICT, CATEGORIES_PATH,
                    "Category name already exists");
        }

        @Test
        void notInGroup_returns400() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("新分類")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(performCreateCategory(request), HttpStatus.BAD_REQUEST, CATEGORIES_PATH,
                    "User is not in a group");

            verify(restaurantCategoryService).createCategory(any(CreateRestaurantCategoryDto.class));
        }

        @Test
        void userNotFound_returns401() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("新分類")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            assertErrorResponse(performCreateCategory(request), HttpStatus.UNAUTHORIZED, CATEGORIES_PATH,
                    "User not found");
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("新分類")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenThrow(new RuntimeException("Create category failed"));

            assertErrorResponse(performCreateCategory(request),
                    HttpStatus.INTERNAL_SERVER_ERROR, CATEGORIES_PATH, "Create category failed");
        }
    }

    @Nested
    class UpdateCategory {

        @Test
        void success_returnsUpdatedCategory() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("下午茶")
                    .displayOrderId(2)
                    .build();
            when(restaurantCategoryService.updateCategory(eq(2), any(UpdateRestaurantCategoryDto.class)))
                    .thenReturn(buildCategoryResponse(2, "下午茶", 2));

            performUpdateCategory(2, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryName").value("下午茶"));

            verify(restaurantCategoryService).updateCategory(eq(2), any(UpdateRestaurantCategoryDto.class));
        }

        @Test
        void success_updateDisplayOrderOnly_returnsUpdatedCategory() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .displayOrderId(3)
                    .build();
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenReturn(buildCategoryResponse(1, "主食", 3));

            performUpdateCategory(1, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayOrderId").value(3));

            verify(restaurantCategoryService).updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class));
        }

        @Test
        void noFieldsToUpdate_returns400() throws Exception {
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update"));

            assertErrorResponse(performUpdateCategory(1, UpdateRestaurantCategoryDto.builder().build()),
                    HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/1", "No fields to update");
        }

        @Test
        void categoryNotFound_returns400() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("不存在")
                    .build();
            when(restaurantCategoryService.updateCategory(eq(999), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));

            assertErrorResponse(performUpdateCategory(999, request),
                    HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/999", "Category not found");
        }

        @Test
        void duplicateName_returns409() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("輕食")
                    .build();
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists"));

            assertErrorResponse(performUpdateCategory(1, request),
                    HttpStatus.CONFLICT, CATEGORIES_PATH + "/1", "Category name already exists");
        }

        @Test
        void displayOrderIdTooSmall_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .displayOrderId(0)
                    .build();

            assertBadRequestValidation(performUpdateCategory(1, request), CATEGORIES_PATH + "/1",
                    "displayOrderId must be greater than or equal to the minimum value");

            verify(restaurantCategoryService, never()).updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class));
        }

        @Test
        void categoryNameTooLong_returns400AndSkipsServiceCall() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("x".repeat(33))
                    .build();

            assertBadRequestValidation(performUpdateCategory(1, request), CATEGORIES_PATH + "/1",
                    "categoryName size is out of allowed range");

            verify(restaurantCategoryService, never()).updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class));
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("測試")
                    .build();

            assertErrorResponseContains(mockMvc.perform(patch(CATEGORIES_PATH + "/{id}", "bad-id")
                            .contentType(CONTENT_TYPE_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(request)))),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, CATEGORIES_PATH + "/bad-id",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void missingBody_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(patch(CATEGORIES_PATH + "/{id}", 1)
                            .contentType(CONTENT_TYPE_JSON)),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, CATEGORIES_PATH + "/1",
                    "Required request body is missing");

            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void invalidJson_returns500AndSkipsServiceCall() throws Exception {
            assertJsonParseError(CATEGORIES_PATH + "/1",
                    mockMvc.perform(patch(CATEGORIES_PATH + "/{id}", 1)
                            .contentType(CONTENT_TYPE_JSON)
                            .content(TRUNCATED_UPDATE_CATEGORY_JSON)));
        }

        @Test
        void notInGroup_returns400() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("新名稱")
                    .build();
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"));

            assertErrorResponse(performUpdateCategory(1, request),
                    HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/1", "User is not in a group");
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            UpdateRestaurantCategoryDto request = UpdateRestaurantCategoryDto.builder()
                    .categoryName("更新失敗")
                    .build();
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new RuntimeException("Update category failed"));

            assertErrorResponse(performUpdateCategory(1, request),
                    HttpStatus.INTERNAL_SERVER_ERROR, CATEGORIES_PATH + "/1", "Update category failed");
        }
    }

    @Nested
    class DeleteCategory {

        @Test
        void success_returns204() throws Exception {
            performDeleteCategory(3).andExpect(status().isNoContent());
            verify(restaurantCategoryService).deleteCategory(3);
        }

        @Test
        void categoryInUse_returns409() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category is in use by restaurants"))
                    .when(restaurantCategoryService).deleteCategory(1);

            assertErrorResponse(performDeleteCategory(1), HttpStatus.CONFLICT, CATEGORIES_PATH + "/1",
                    "Category is in use by restaurants");
        }

        @Test
        void lastCategory_returns409() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last category"))
                    .when(restaurantCategoryService).deleteCategory(1);

            assertErrorResponse(performDeleteCategory(1), HttpStatus.CONFLICT, CATEGORIES_PATH + "/1",
                    "Cannot delete the last category");
        }

        @Test
        void categoryNotFound_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"))
                    .when(restaurantCategoryService).deleteCategory(999);

            assertErrorResponse(performDeleteCategory(999), HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/999",
                    "Category not found");
        }

        @Test
        void notInGroup_returns400() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group"))
                    .when(restaurantCategoryService).deleteCategory(1);

            assertErrorResponse(performDeleteCategory(1), HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/1",
                    "User is not in a group");
        }

        @Test
        void notGroupAdmin_returns403() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action"))
                    .when(restaurantCategoryService).deleteCategory(1);

            assertErrorResponse(performDeleteCategory(1), HttpStatus.FORBIDDEN, CATEGORIES_PATH + "/1",
                    "Only group admin can perform this action");
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(delete(CATEGORIES_PATH + "/{id}", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, CATEGORIES_PATH + "/bad-id",
                    "Failed to convert value of type");

            verifyNoInteractions(restaurantCategoryService);
        }

        @Test
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            doThrow(new RuntimeException("Delete category failed"))
                    .when(restaurantCategoryService).deleteCategory(1);

            assertErrorResponse(performDeleteCategory(1),
                    HttpStatus.INTERNAL_SERVER_ERROR, CATEGORIES_PATH + "/1", "Delete category failed");
        }
    }

    private void assertBadRequestValidation(ResultActions resultActions, String path, String... messageParts)
            throws Exception {
        String responseJson = resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(CODE_BAD_REQUEST))
                .andExpect(jsonPath("$.path").value(path))
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (String part : messageParts) {
            assertMessageContains(responseJson, part);
        }
    }

    private void assertMessageContains(String responseJson, String messagePart) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode messageNode = rootNode.get("message");
        assertNotNull(messageNode, "message field should exist");
        assertTrue(messageNode.asText().contains(messagePart),
                "message should contain: " + messagePart + " but was: " + messageNode.asText());
    }

    private void assertMissingJsonBodyError(@NonNull String path) throws Exception {
        assertErrorResponseContains(
                mockMvc.perform(post(path).contentType(CONTENT_TYPE_JSON)),
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                path,
                "Required request body is missing");
        verifyNoInteractions(restaurantCategoryService);
    }

    private void assertJsonParseError(@NonNull String path, ResultActions truncatedJsonPost) throws Exception {
        assertErrorResponseContains(
                truncatedJsonPost,
                HTTP_INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_SERVER_ERROR,
                path,
                "JSON parse error");
        verifyNoInteractions(restaurantCategoryService);
    }

    private ResultActions performCreateCategory(CreateRestaurantCategoryDto request) throws Exception {
        return mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performCreateCategoryRaw(String payload) throws Exception {
        return mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(payload)));
    }

    private ResultActions performUpdateCategory(Integer id, UpdateRestaurantCategoryDto request) throws Exception {
        return mockMvc.perform(patch(CATEGORIES_PATH + "/{id}", id)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performDeleteCategory(Integer id) throws Exception {
        return mockMvc.perform(delete(CATEGORIES_PATH + "/{id}", id));
    }

    private static RestaurantCategoryResponse buildCategoryResponse(
            Integer categoryId, String categoryName, Integer displayOrderId, long restaurantCount) {
        return RestaurantCategoryResponse.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .displayOrderId(displayOrderId)
                .restaurantCount(restaurantCount)
                .build();
    }

    private static RestaurantCategoryResponse buildCategoryResponse(
            Integer categoryId, String categoryName, Integer displayOrderId) {
        return buildCategoryResponse(categoryId, categoryName, displayOrderId, 0L);
    }
}
