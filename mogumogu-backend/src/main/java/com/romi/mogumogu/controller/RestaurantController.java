package com.romi.mogumogu.controller;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCreateRequest;
import com.romi.mogumogu.dto.restaurant.RestaurantMapResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantUpdateRequest;
import com.romi.mogumogu.service.RestaurantMapService;
import com.romi.mogumogu.service.RestaurantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurants")
@Tag(name = "restaurants", description = "餐廳")
public class RestaurantController {
    private final RestaurantService restaurantService;
    private final RestaurantMapService restaurantMapService;

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("")
    @Operation(summary = "取得餐廳清單(系統管理員)")
    public PageResponse<RestaurantResponse> getRestaurants(
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @ParameterObject Pageable pageable) {
        return restaurantService.getRestaurants(groupId, keyword, categoryId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "依餐廳ID取得餐廳資料")
    public RestaurantResponse getRestaurantById(@PathVariable("id") Integer restaurantId) {
        return restaurantService.getRestaurantById(restaurantId);
    }

    @GetMapping("group/{id}")
    @Operation(summary = "取得自己所屬群組的餐廳清單")
    public PageResponse<RestaurantResponse> getRestaurantsByGroupId(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @ParameterObject @PageableDefault(sort = "displayOrderId") Pageable pageable) {
        return restaurantService.getRestaurantsByGroupId(keyword, categoryId, pageable);
    }

    @PostMapping("")
    @Operation(summary = "新增餐廳")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantCreateRequest restaurantCreateRequest) {
        return restaurantService.createRestaurant(restaurantCreateRequest);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改餐廳")
    public RestaurantResponse updateRestaurant(
            @PathVariable("id") Integer restaurantId,
            @RequestBody @Valid RestaurantUpdateRequest restaurantUpdateRequest) {
        return restaurantService.updateRestaurant(restaurantId, restaurantUpdateRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐廳")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestaurant(@PathVariable("id") Integer restaurantId) {
        restaurantService.deleteRestaurant(restaurantId);
    }

    /** 搜尋附近餐廳 */
    @GetMapping("/map")
    @Operation(summary = "搜尋附近餐廳")
    public RestaurantMapResponse getMapRestaurants(
            @RequestParam(defaultValue = "25.0478") @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam(defaultValue = "121.517") @DecimalMin("-180") @DecimalMax("180") BigDecimal lon,
            @RequestParam(defaultValue = "50") @Min(1) @Max(3000) int radius) {
        return restaurantMapService.getMapRestaurants(lat.doubleValue(), lon.doubleValue(), radius);
    }
}
