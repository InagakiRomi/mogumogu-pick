package com.romi.mogumogu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.data.domain.Sort;

/** 餐廳清單排序：欄位與升降冪（對應前端 GatheringSortBy / GatheringSortOrder） */
public final class RestaurantSort {

    private RestaurantSort() {}

    /** 餐廳排序欄位 */
    @Getter
    @AllArgsConstructor
    public enum SortBy {
        /** 餐廳 ID */
        RESTAURANT_ID("restaurantId"),
        /** 餐廳分類 ID */
        CATEGORY_ID("categoryId"),
        /** 顯示順序 */
        DISPLAY_ORDER("displayOrder"),
        /** 選擇次數 */
        SELECTED_COUNT("selectedCount"),
        /** 餐廳名稱 */
        RESTAURANT_NAME("restaurantName"),
        /** 最後選擇時間 */
        LAST_SELECTED_AT("lastSelectedAt"),
        /** 建立時間 */
        CREATED_AT("createdAt"),
        /** 更新時間 */
        UPDATED_AT("updatedAt");

        private final String sortProperty;
    }

    /** 餐廳排序方式 */
    public enum SortOrder {
        /** 升冪 */
        ASC(Sort.Direction.ASC),
        /** 降冪 */
        DESC(Sort.Direction.DESC);

        private final Sort.Direction sortDirection;

        SortOrder(Sort.Direction sortDirection) {
            this.sortDirection = sortDirection;
        }

        public Sort.Direction getSortDirection() {
            return sortDirection;
        }

        public static SortOrder from(String value) {
            if (value == null || value.isBlank()) {
                return ASC;
            }
            if ("DESC".equalsIgnoreCase(value.trim())) {
                return DESC;
            }
            return ASC;
        }
    }
}
