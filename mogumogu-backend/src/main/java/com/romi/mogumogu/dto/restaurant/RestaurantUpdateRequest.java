package com.romi.mogumogu.dto.restaurant;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RestaurantUpdateRequest(
        @Min(1) @Schema(example = "1") Integer categoryId,
        @Min(1) @Schema(example = "100") Integer displayOrderId,
        @Min(0) @Schema(example = "0") Integer selectedCount,
        @Size(max = 64) @Schema(example = "和食天國") String restaurantName,
        @Size(max = 255) @Schema(example = "台北市信義區信義路五段7號") String address,
        @Size(max = 512) @Schema(example = "可訂位") String note,
        @Size(max = 512) @Schema(example = "https://example.com/restaurant.jpg") String imageUrl,
        @Schema(example = "2026-11-27 12:00:00") LocalDateTime lastSelectedAt) {
}