package com.romi.mogumogu.Response;

import com.romi.mogumogu.entity.dish.DishEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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

    public static DishResponse dishResponse(DishEntity entity) {
        return DishResponse.builder()
                .dishId(entity.getDishId())
                .restaurantId(entity.getRestaurantId().getRestaurantId())
                .displayOrderId(entity.getDisplayOrderId())
                .price(entity.getPrice())
                .dishName(entity.getDishName())
                .build();
    }
}
