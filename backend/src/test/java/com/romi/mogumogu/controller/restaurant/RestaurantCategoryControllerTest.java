package com.romi.mogumogu.controller.restaurant;

import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponse;
import static com.romi.mogumogu.testutil.ErrorResponseTestUtils.assertErrorResponseContains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;

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
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";

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
        void duplicateName_returns409() throws Exception {
            CreateRestaurantCategoryDto request = CreateRestaurantCategoryDto.builder()
                    .categoryName("主食")
                    .build();
            when(restaurantCategoryService.createCategory(any(CreateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists"));

            assertErrorResponse(performCreateCategory(request), HttpStatus.CONFLICT, CATEGORIES_PATH,
                    "Category name already exists");
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
        void noFieldsToUpdate_returns400() throws Exception {
            when(restaurantCategoryService.updateCategory(eq(1), any(UpdateRestaurantCategoryDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update"));

            assertErrorResponse(performUpdateCategory(1, UpdateRestaurantCategoryDto.builder().build()),
                    HttpStatus.BAD_REQUEST, CATEGORIES_PATH + "/1", "No fields to update");
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
    }

    private ResultActions performCreateCategory(CreateRestaurantCategoryDto request) throws Exception {
        return mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
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
