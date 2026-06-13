package com.romi.mogumogu.Response;

import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantCategoryResponse {
    @Schema(description = "分類 ID", example = "1")
    private Integer categoryId;

    @Schema(description = "分類名稱", example = "主食")
    private String categoryName;

    @Schema(description = "群組內顯示排序 ID", example = "1")
    private Integer displayOrderId;

    @Schema(description = "使用此分類的餐廳數量", example = "3")
    private Long restaurantCount;

    public static RestaurantCategoryResponse from(RestaurantCategoryEntity entity, long restaurantCount) {
        return RestaurantCategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .displayOrderId(entity.getDisplayOrderId())
                .restaurantCount(restaurantCount)
                .build();
    }
}
