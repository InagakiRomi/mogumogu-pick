package com.romi.mogumogu.controller.restaurant;

import com.romi.mogumogu.Response.DishListResponse;
import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.Response.SelectionHistoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
import com.romi.mogumogu.dto.GetSelectionHistoryQuery;
import com.romi.mogumogu.dto.UpdateRestaurantDto;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants")
@Tag(name = "restaurants", description = "餐廳")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("")
    @Operation(summary = "取得所有餐廳清單")
    public RestaurantListResponse<RestaurantResponse> getRestaurants(
            @Valid @ModelAttribute @ParameterObject GetRestaurantQuery queryParams) {
        return restaurantService.getRestaurants(queryParams);
    }

    @GetMapping("/my")
    @Operation(summary = "取得自己所屬群組的餐廳清單")
    public RestaurantListResponse<RestaurantResponse> getMyGroupRestaurants(
            @Valid @ModelAttribute @ParameterObject GetRestaurantQuery queryParams) {
        return restaurantService.getMyGroupRestaurants(queryParams);
    }

    @GetMapping("/my/random")
    @Operation(summary = "抽取自己所屬群組的一間餐廳")
    public RestaurantResponse getRandomMyGroupRestaurant(@RequestParam(required = false) Integer categoryId) {
        return restaurantService.getRandomMyGroupRestaurant(categoryId);
    }

    @PatchMapping("/my/choose/{id}")
    @Operation(summary = "確認選擇餐廳並重置抽籤池")
    public RestaurantResponse chooseMyGroupRestaurant(@PathVariable("id") Integer restaurantId) {
        return restaurantService.chooseMyGroupRestaurant(restaurantId);
    }

    @PostMapping("/my/random/clear")
    @Operation(summary = "重置抽籤池")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMyGroupRandomPool() {
        restaurantService.clearMyGroupRandomPool();
    }

    @GetMapping("/my/selection-history")
    @Operation(summary = "查詢自己所屬群組的餐廳選擇歷史")
    public RestaurantListResponse<SelectionHistoryResponse> getMyGroupSelectionHistory(
            @Valid @ModelAttribute @ParameterObject GetSelectionHistoryQuery queryParams) {
        return restaurantService.getMyGroupSelectionHistory(queryParams);
    }

    @GetMapping("/{id}")
    @Operation(summary = "依餐廳 ID 取得自己所屬群組的單筆餐廳資訊")
    public RestaurantResponse getRestaurant(@PathVariable("id") Integer restaurantId) {
        return restaurantService.getRestaurant(restaurantId);
    }

    @GetMapping("/{restaurantId}/dishes")
    @Operation(summary = "查詢該餐廳對應的所有餐點")
    public DishListResponse getRestaurantDishes(@PathVariable Integer restaurantId) {
        return restaurantService.getRestaurantDishes(restaurantId);
    }

    @PostMapping("")
    @Operation(summary = "新增餐廳")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(@Valid @RequestBody CreateRestaurantDto request) {
        return restaurantService.createRestaurant(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改餐廳")
    public RestaurantResponse updateRestaurant(
            @PathVariable("id") Integer restaurantId,
            @RequestBody @Valid UpdateRestaurantDto request) {
        return restaurantService.updateRestaurant(restaurantId, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐廳（軟刪除）")
    @ResponseStatus(HttpStatus.OK)
    public RestaurantResponse deleteRestaurant(@PathVariable("id") Integer restaurantId) {
        return restaurantService.deleteRestaurant(restaurantId);
    }
}
