package com.romi.mogumogu.dto;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RestaurantDto {
        @Schema(description = "餐廳 ID", example = "1")
        private Integer restaurantId;

        @Schema(description = "分組 ID", example = "1")
        private Integer groupId;

        @Schema(description = "分類 ID", example = "1")
        private Integer categoryId;

        @Schema(description = "顯示排序", example = "1")
        private Integer displayOrder;

        @Schema(description = "餐廳名稱", example = "壽司郎")
        private String restaurantName;

        @Schema(description = "備註", example = "可訂位")
        private String note;

        @Schema(description = "圖片網址", example = "https://example.com/restaurant.jpg")
        private String imageUrl;

        @Schema(description = "被選取次數", example = "10")
        private Integer selectedCount;

        @Schema(description = "最後被選取時間", pattern = "yyyy-MM-dd HH:mm:ss", example = "2026-05-03 14:58:57")
        private Date lastSelectedAt;

        @Schema(description = "建立時間", pattern = "yyyy-MM-dd HH:mm:ss", example = "2026-05-03 14:58:57")
        private Date createdAt;

        @Schema(description = "更新時間", pattern = "yyyy-MM-dd HH:mm:ss", example = "2026-05-03 14:58:57")
        private Date updatedAt;

        public static RestaurantDto restaurantResponse(RestaurantEntity entity) {
                return RestaurantDto.builder()
                                .restaurantId(entity.getRestaurantId())
                                .groupId(entity.getGroupId())
                                .categoryId(entity.getCategoryId().getCategoryId())
                                .displayOrder(entity.getDisplayOrder())
                                .restaurantName(entity.getRestaurantName())
                                .note(entity.getNote())
                                .imageUrl(entity.getImageUrl())
                                .selectedCount(entity.getSelectedCount())
                                .lastSelectedAt(entity.getLastSelectedAt())
                                .createdAt(entity.getCreatedAt())
                                .updatedAt(entity.getUpdatedAt())
                                .build();
        }
}
