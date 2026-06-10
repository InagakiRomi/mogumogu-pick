package com.romi.mogumogu.controller.dish;

import com.romi.mogumogu.Response.DishResponse;
import com.romi.mogumogu.dto.CreateDishDto;
import com.romi.mogumogu.dto.UpdateDishNameDto;
import com.romi.mogumogu.service.dish.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dishes")
@Tag(name = "dishes", description = "餐點")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @PostMapping("")
    @Operation(summary = "新增餐點")
    @ResponseStatus(HttpStatus.CREATED)
    public DishResponse createDish(@Valid @RequestBody CreateDishDto request) {
        return dishService.createDish(request);
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "修改餐點名稱")
    public DishResponse updateDishName(@PathVariable("id") Integer dishId, @Valid @RequestBody UpdateDishNameDto request) {
        return dishService.updateDishName(dishId, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除餐點")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDish(@PathVariable("id") Integer dishId) {
        dishService.deleteDish(dishId);
    }
}
