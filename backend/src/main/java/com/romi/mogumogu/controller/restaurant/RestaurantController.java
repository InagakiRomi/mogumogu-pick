package com.romi.mogumogu.controller.restaurant;

import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
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
    public RestaurantListResponse getRestaurants(
            @Valid @ModelAttribute @ParameterObject GetRestaurantQuery queryParams) {
        return restaurantService.getRestaurants(queryParams);
    }

    @GetMapping("/my")
    @Operation(summary = "取得自己所屬群組的餐廳清單")
    public RestaurantListResponse getMyGroupRestaurants(
            @Valid @ModelAttribute @ParameterObject GetRestaurantQuery queryParams) {
        return restaurantService.getMyGroupRestaurants(queryParams);
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
