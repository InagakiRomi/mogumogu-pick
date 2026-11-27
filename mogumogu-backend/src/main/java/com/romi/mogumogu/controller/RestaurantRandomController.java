package com.romi.mogumogu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.dto.restaurant.RestaurantRandomResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantResponse;
import com.romi.mogumogu.service.RestaurantRandomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurants/random")
@Tag(name = "restaurant-random", description = "餐廳抽選")
public class RestaurantRandomController {
    private final RestaurantRandomService restaurantRandomService;

    @PostMapping("")
    @Operation(summary = "抽取自己所屬群組的一間餐廳")
    public RestaurantRandomResponse getRandomRestaurant(
            @RequestParam(defaultValue = "") List<Integer> randomPool,
            @RequestParam(required = false) Integer categoryId) {
        return restaurantRandomService.getRandomRestaurant(randomPool, categoryId);
    }

    @PostMapping("/choose/{id}")
    @Operation(summary = "確認選擇餐廳並重置抽籤池")
    public RestaurantResponse chooseRestaurant(
            @RequestParam(defaultValue = "") List<Integer> randomPool,
            @PathVariable("id") Integer restaurantId) {
        return restaurantRandomService.chooseRestaurant(randomPool, restaurantId);
    }

    @PostMapping("/clear")
    @Operation(summary = "重置抽籤池")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearRandomPool(@RequestParam(defaultValue = "") List<Integer> randomPool) {
        restaurantRandomService.clearRandomPool(randomPool);
    }
}
