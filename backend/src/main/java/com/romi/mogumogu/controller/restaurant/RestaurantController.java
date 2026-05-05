package com.romi.mogumogu.controller.restaurant;

import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@Tag(name = "restaurants", description = "餐廳")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Operation(summary = "取得餐廳清單", description = "查詢餐廳列表")
    @GetMapping("")
    public List<RestaurantResponse> getRestaurants() {
        return restaurantService.getRestaurants();
    }

    @Operation(summary = "新增餐廳", description = "建立餐廳；分類須存在且所屬群組須與請求中的群組一致")
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(@Valid @RequestBody CreateRestaurantDto request) {
        return restaurantService.createRestaurant(request);
    }
}
