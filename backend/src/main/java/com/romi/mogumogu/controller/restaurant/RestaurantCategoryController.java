package com.romi.mogumogu.controller.restaurant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.Response.RestaurantCategoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantCategoryDto;
import com.romi.mogumogu.dto.UpdateRestaurantCategoryDto;
import com.romi.mogumogu.service.restaurant.RestaurantCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/restaurant-categories")
@Tag(name = "restaurant-categories", description = "餐廳分類")
public class RestaurantCategoryController {

    private final RestaurantCategoryService restaurantCategoryService;

    public RestaurantCategoryController(RestaurantCategoryService restaurantCategoryService) {
        this.restaurantCategoryService = restaurantCategoryService;
    }

    @GetMapping("")
    @Operation(summary = "取得自己所屬群組的餐廳分類清單")
    public List<RestaurantCategoryResponse> getMyGroupCategories() {
        return restaurantCategoryService.getMyGroupCategories();
    }

    @PostMapping("")
    @Operation(summary = "新增餐廳分類")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantCategoryResponse createCategory(@Valid @RequestBody CreateRestaurantCategoryDto request) {
        return restaurantCategoryService.createCategory(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改餐廳分類")
    public RestaurantCategoryResponse updateCategory(
            @PathVariable("id") Integer categoryId,
            @Valid @RequestBody UpdateRestaurantCategoryDto request) {
        return restaurantCategoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐廳分類")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("id") Integer categoryId) {
        restaurantCategoryService.deleteCategory(categoryId);
    }
}
