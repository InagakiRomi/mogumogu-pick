package com.romi.mogumogu.dto.restaurant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestaurantCreateRequest(
        @NotNull @Min(1) @Schema(example = "1") Integer categoryId,
        @NotBlank @Size(max = 64) @Schema(example = "和食天國") String restaurantName,
        @Size(max = 255) @Schema(example = "台北市信義區信義路五段7號") String address,
        @Size(max = 512) @Schema(example = "可訂位") String note,
        @Size(max = 512) String imageUrl) {
}