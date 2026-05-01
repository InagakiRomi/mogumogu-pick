package com.romi.mogumogu.controller.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.service.restaurant.RestaurantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("")
    public List<RestaurantEntity> getRestaurants() {
        return restaurantService.getRestaurants();
    }
}
