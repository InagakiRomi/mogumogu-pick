package com.romi.mogumogu.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.service.RestaurantCategoryService;
import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCategoryResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCategoryUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant-categories")
@Tag(name = "restaurant-categories", description = "餐廳分類")
public class RestaurantCategoryController {
    private final RestaurantCategoryService restaurantCategoryService;

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("")
    @Operation(summary = "取得所有餐廳分類(系統管理員)")
    public PageResponse<RestaurantCategoryResponse> getCategories(@ParameterObject Pageable pageable) {
        return restaurantCategoryService.getCategories(pageable);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "取得指定群組的餐廳分類清單")
    public PageResponse<RestaurantCategoryResponse> getCategoriesByGroupId(
            @PathVariable Integer groupId,
            @ParameterObject Pageable pageable) {
        return restaurantCategoryService.getCategoriesByGroupId(groupId, pageable);
    }

    @PostMapping(value = "/group", consumes = "text/plain")
    @Operation(summary = "新增目前使用者群組的分類")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantCategoryResponse createGroupCategory(
            @Schema(example = "日式料理") @NotBlank @Size(max = 32) @RequestBody String categoryName) {
        return restaurantCategoryService.createGroupCategory(categoryName);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改餐廳分類")
    public RestaurantCategoryResponse updateCategory(
            @PathVariable("id") Integer categoryId,
            @Valid @RequestBody RestaurantCategoryUpdateRequest restaurantCategoryUpdateRequest) {
        return restaurantCategoryService.updateCategory(categoryId, restaurantCategoryUpdateRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐廳分類")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("id") Integer categoryId) {
        restaurantCategoryService.deleteCategory(categoryId);
    }
}
