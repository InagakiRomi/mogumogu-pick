package com.romi.mogumogu.controller.dish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.romi.mogumogu.Response.DishListResponse;
import com.romi.mogumogu.Response.DishResponse;
import com.romi.mogumogu.dto.CreateDishDto;
import com.romi.mogumogu.dto.UpdateDishNameDto;
import com.romi.mogumogu.exception.GlobalExceptionHandler;
import com.romi.mogumogu.service.dish.DishService;
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

import java.util.List;
import java.util.Objects;

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

@WebMvcTest(controllers = DishController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class DishControllerTest {

    private static final String DISHES_PATH = "/dishes";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String CODE_BAD_REQUEST = "BAD_REQUEST";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DishService dishService;

    @Nested
    class CreateDish {
        @Test
        void success_returns201AndCreatedDish() throws Exception {
            CreateDishDto request = CreateDishDto.builder()
                    .restaurantId(1)
                    .price(130)
                    .dishName("牛肉拉麵")
                    .build();
            when(dishService.createDish(any(CreateDishDto.class)))
                    .thenReturn(buildDishResponse(11, 1, 1, 130, "牛肉拉麵"));

            performCreateDish(request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dishId").value(11))
                    .andExpect(jsonPath("$.restaurantId").value(1))
                    .andExpect(jsonPath("$.displayOrderId").value(1))
                    .andExpect(jsonPath("$.price").value(130))
                    .andExpect(jsonPath("$.dishName").value("牛肉拉麵"));

            verify(dishService).createDish(any(CreateDishDto.class));
        }

        @Test
        void validationFailed_returns400AndSkipsServiceCall() throws Exception {
            CreateDishDto request = CreateDishDto.builder()
                    .restaurantId(-1)
                    .price(null)
                    .dishName("")
                    .build();

            assertErrorResponseContains(performCreateDish(request),
                    400, CODE_BAD_REQUEST, DISHES_PATH, "restaurantId must be greater than or equal to the minimum value");
            assertErrorResponseContains(performCreateDish(request),
                    400, CODE_BAD_REQUEST, DISHES_PATH, "price is required");
            assertErrorResponseContains(performCreateDish(request),
                    400, CODE_BAD_REQUEST, DISHES_PATH, "dishName is required");

            verifyNoInteractions(dishService);
        }

        @Test
        void restaurantNotFound_returns404() throws Exception {
            CreateDishDto request = CreateDishDto.builder()
                    .restaurantId(999)
                    .price(90)
                    .dishName("不存在餐廳餐點")
                    .build();
            when(dishService.createDish(any(CreateDishDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performCreateDish(request),
                    HttpStatus.NOT_FOUND, DISHES_PATH, "Restaurant not found");

            verify(dishService).createDish(any(CreateDishDto.class));
        }
    }

    @Nested
    class UpdateDishName {
        @Test
        void success_returns200WithUpdatedName() throws Exception {
            UpdateDishNameDto request = UpdateDishNameDto.builder().dishName("雙倍叉燒拉麵").build();
            when(dishService.updateDishName(eq(12), any(UpdateDishNameDto.class)))
                    .thenReturn(buildDishResponse(12, 1, 2, 180, "雙倍叉燒拉麵"));

            performUpdateDishName(12, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dishId").value(12))
                    .andExpect(jsonPath("$.dishName").value("雙倍叉燒拉麵"));

            verify(dishService).updateDishName(eq(12), any(UpdateDishNameDto.class));
        }

        @Test
        void validationFailed_returns400AndSkipsServiceCall() throws Exception {
            UpdateDishNameDto request = UpdateDishNameDto.builder().dishName("").build();
            assertErrorResponseContains(performUpdateDishName(10, request),
                    400, CODE_BAD_REQUEST, "/dishes/10/name", "dishName is required");
            verifyNoInteractions(dishService);
        }

        @Test
        void dishNotFound_returns404() throws Exception {
            UpdateDishNameDto request = UpdateDishNameDto.builder().dishName("更新失敗").build();
            when(dishService.updateDishName(eq(404), any(UpdateDishNameDto.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));

            assertErrorResponse(performUpdateDishName(404, request),
                    HttpStatus.NOT_FOUND, "/dishes/404/name", "Dish not found");
        }
    }

    @Nested
    class GetRestaurantDishes {
        @Test
        void success_returnsDishList() throws Exception {
            DishResponse first = buildDishResponse(1, 100, 1, 120, "豚骨拉麵");
            DishResponse second = buildDishResponse(2, 100, 2, 90, "炸蝦天婦羅");
            when(dishService.getRestaurantDishes(100))
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

            verify(dishService).getRestaurantDishes(100);
        }

        @Test
        void restaurantNotFound_returns404() throws Exception {
            when(dishService.getRestaurantDishes(999))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

            assertErrorResponse(performGetRestaurantDishes(999),
                    HttpStatus.NOT_FOUND, "/restaurants/999/dishes", "Restaurant not found");

            verify(dishService).getRestaurantDishes(999);
        }
    }

    @Nested
    class DeleteDish {
        @Test
        void success_returns204() throws Exception {
            performDeleteDish(12)
                    .andExpect(status().isNoContent());
            verify(dishService).deleteDish(12);
        }

        @Test
        void dishNotFound_returns404() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"))
                    .when(dishService).deleteDish(404);

            assertErrorResponse(performDeleteDish(404),
                    HttpStatus.NOT_FOUND, "/dishes/404", "Dish not found");
            verify(dishService).deleteDish(404);
        }

        @Test
        void invalidPathVariable_returns500AndSkipsServiceCall() throws Exception {
            assertErrorResponseContains(mockMvc.perform(delete("/dishes/{id}", "bad-id")),
                    HTTP_INTERNAL_SERVER_ERROR, CODE_INTERNAL_SERVER_ERROR, "/dishes/bad-id",
                    "Failed to convert value of type");
            verifyNoInteractions(dishService);
        }
    }

    private ResultActions performCreateDish(Object request) throws Exception {
        return mockMvc.perform(post(DISHES_PATH)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performUpdateDishName(Integer dishId, Object request) throws Exception {
        return mockMvc.perform(patch("/dishes/{id}/name", dishId)
                .contentType(CONTENT_TYPE_JSON)
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))));
    }

    private ResultActions performGetRestaurantDishes(Integer restaurantId) throws Exception {
        return mockMvc.perform(get("/restaurants/{restaurantId}/dishes", restaurantId));
    }

    private ResultActions performDeleteDish(Integer dishId) throws Exception {
        return mockMvc.perform(delete("/dishes/{id}", dishId));
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
}
