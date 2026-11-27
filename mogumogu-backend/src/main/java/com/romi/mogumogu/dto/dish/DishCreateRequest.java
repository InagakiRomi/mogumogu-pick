package com.romi.mogumogu.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DishCreateRequest(
        @Schema(example = "1") @NotNull @Min(1) Integer restaurantId,
        @Schema(example = "130") @NotNull @Min(0) Integer price,
        @Schema(example = "牛肉拉麵") @Size(max = 64) @NotBlank String dishName) {
}