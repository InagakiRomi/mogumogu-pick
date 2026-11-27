package com.romi.mogumogu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.dish.DishCreateRequest;
import com.romi.mogumogu.dto.dish.DishResponse;
import com.romi.mogumogu.dto.dish.DishUpdateRequest;
import com.romi.mogumogu.service.DishService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dishes")
@Tag(name = "dishes", description = "餐點")
public class DishController {
    private final DishService dishService;

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "取得指定餐廳的餐點清單")
    public PageResponse<DishResponse> getDishesByRestaurantId(
            @PathVariable Integer restaurantId,
            @ParameterObject Pageable pageable) {
        return dishService.getDishesByRestaurantId(restaurantId, pageable);
    }

    @PostMapping("")
    @Operation(summary = "新增餐點")
    @ResponseStatus(HttpStatus.CREATED)
    public DishResponse createDish(@Valid @RequestBody DishCreateRequest dishCreateRequest) {
        return dishService.createDish(dishCreateRequest);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改餐點")
    public DishResponse updateDish(
            @PathVariable("id") Integer dishId,
            @Valid @RequestBody DishUpdateRequest dishUpdateRequest) {
        return dishService.updateDish(dishId, dishUpdateRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐點")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDish(@PathVariable("id") Integer dishId) {
        dishService.deleteDish(dishId);
    }
}
