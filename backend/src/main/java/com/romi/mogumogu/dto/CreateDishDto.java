package com.romi.mogumogu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDishDto {

    @NotNull
    @Min(0)
    @Schema(description = "餐點所屬餐廳 ID", example = "1")
    private Integer restaurantId;

    @NotNull
    @Min(0)
    @Schema(description = "餐點價格", example = "130")
    private Integer price;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "餐點名稱", example = "牛肉拉麵")
    private String dishName;
}
