package com.romi.mogumogu.Response;

import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;

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
public class SelectionHistoryResponse {
    /** 歷史紀錄 ID */
    private Integer historyId;

    /** 餐廳 ID */
    private Integer restaurantId;

    /** 餐延名稱 */
    private String restaurantName;

    /** 分類名稱 */
    private String category;

    /** 選擇時間 */
    private Date selectedAt;

    public static SelectionHistoryResponse from(RestaurantSelectionHistoryEntity entity) {
        return SelectionHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .restaurantId(entity.getRestaurant().getRestaurantId())
                .restaurantName(entity.getRestaurant().getRestaurantName())
                .category(entity.getRestaurant().getCategoryId().getCategoryName())
                .selectedAt(entity.getSelectedAt())
                .build();
    }
}
