package com.romi.mogumogu.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantSelectionResponse;
import com.romi.mogumogu.service.RestaurantSelectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant-selections")
@Tag(name = "restaurant-selections", description = "餐廳選擇歷史")
public class RestaurantSelectionController {
    private final RestaurantSelectionService restaurantSelectionService;

    @GetMapping("/group/{groupId}")
    @Operation(summary = "取得指定群組的餐廳選擇歷史")
    public PageResponse<RestaurantSelectionResponse> getSelectionsByGroupId(
            @PathVariable Integer groupId,
            @ParameterObject Pageable pageable) {
        return restaurantSelectionService.getSelectionsByGroupId(groupId, pageable);
    }

    @PostMapping("/group")
    @Operation(summary = "新增目前使用者群組的餐廳選擇紀錄")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantSelectionResponse createGroupSelection(@NotNull @Positive Integer restaurantId) {
        return restaurantSelectionService.createGroupSelection(restaurantId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除單筆餐廳選擇紀錄")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSelection(@PathVariable("id") Integer selectionId) {
        restaurantSelectionService.deleteSelection(selectionId);
    }
}