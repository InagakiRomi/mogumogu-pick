package com.romi.mogumogu.controller.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("")
    @Operation(summary = "取得餐廳清單", description = "查詢餐廳列表")
    public List<RestaurantEntity> getRestaurants() {
        return restaurantService.getRestaurants();
    }
}
