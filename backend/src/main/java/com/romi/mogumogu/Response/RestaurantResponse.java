package com.romi.mogumogu.Response;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.constant.DateTimePatternConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
        @Schema(description = "餐廳 ID", example = "1")
        private Integer restaurantId;

        @Schema(description = "分組 ID", example = "1")
        private Integer groupId;

        @Schema(description = "分類 ID", example = "1")
        private Integer categoryId;

        @Schema(description = "群組內顯示排序 ID", example = "1")
        private Integer displayOrderId;

        @Schema(description = "被選取次數", example = "10")
        private Integer selectedCount;

        @Schema(description = "餐廳名稱", example = "和食天國")
        private String restaurantName;

        @Schema(description = "備註", example = "可訂位")
        private String note;

        @Schema(description = "圖片網址", example = "https://example.com/restaurant.jpg")
        private String imageUrl;

        @Schema(description = "是否封存（軟刪除）", example = "false")
        private Boolean isArchived;

        @Schema(description = "最後被選取時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME, example = "2026-05-03 14:58:57")
        private Date lastSelectedAt;

        @Schema(description = "建立時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME, example = "2026-05-03 14:58:57")
        private Date createdAt;

        @Schema(description = "更新時間", pattern = DateTimePatternConstants.STANDARD_DATE_TIME, example = "2026-05-03 14:58:57")
        private Date updatedAt;

        public static RestaurantResponse restaurantResponse(RestaurantEntity entity) {
                return RestaurantResponse.builder()
                                .restaurantId(entity.getRestaurantId())
                                .groupId(entity.getGroupId())
                                .categoryId(entity.getCategoryId().getCategoryId())
                                .displayOrderId(entity.getDisplayOrderId())
                                .selectedCount(entity.getSelectedCount())
                                .restaurantName(entity.getRestaurantName())
                                .note(entity.getNote())
                                .imageUrl(entity.getImageUrl())
                                .isArchived(entity.getIsArchived())
                                .lastSelectedAt(entity.getLastSelectedAt())
                                .createdAt(entity.getCreatedAt())
                                .updatedAt(entity.getUpdatedAt())
                                .build();
        }
}
