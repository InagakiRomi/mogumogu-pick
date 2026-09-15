package com.romi.mogumogu.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishResponse {
    @Schema(description = "餐點 ID", example = "1")
    private Integer dishId;

    @Schema(description = "餐點對應餐廳編號", example = "1")
    private Integer restaurantId;

    @Schema(description = "餐廳群組內順序 ID", example = "1")
    private Integer displayOrderId;

    @Schema(description = "價格", example = "120")
    private Integer price;

    @Schema(description = "餐點名稱", example = "豚骨拉麵")
    private String dishName;
}
