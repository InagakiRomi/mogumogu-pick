package com.romi.mogumogu.dto.restaurant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RestaurantCategoryUpdateRequest(
        @Size(max = 32) @Schema(example = "韓式料理") String categoryName,
        @Schema(example = "1") @Min(1) Integer displayOrderId) {
}
