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
public class UpdateDishDto {

    @NotNull
    @Min(1)
    @Schema(description = "餐廳群組內順序 ID", example = "1")
    private Integer displayOrderId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "餐點名稱", example = "雙倍叉燒拉麵")
    private String dishName;

    @NotNull
    @Min(0)
    @Schema(description = "餐點價格", example = "180")
    private Integer price;
}
