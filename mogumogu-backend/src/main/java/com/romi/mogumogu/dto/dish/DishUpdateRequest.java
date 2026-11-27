package com.romi.mogumogu.dto.dish;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DishUpdateRequest(
        @Min(1) Integer restaurantId,
        @Min(0) Integer price,
        @Size(max = 64) String dishName,
        @Min(1) Integer displayOrderId) {
}