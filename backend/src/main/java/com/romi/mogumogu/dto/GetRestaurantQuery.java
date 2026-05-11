package com.romi.mogumogu.dto;

import com.romi.mogumogu.enums.RestaurantSort;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantQuery {
    @Schema(description = "群組 ID")
    private Integer groupId;

    @Schema(description = "分類 ID")
    private Integer categoryId;

    @Schema(description = "是否已刪除")
    private Boolean isArchived;

    @Schema(description = "搜尋關鍵字")
    private String search;

    @Schema(description = "排序欄位")
    @Default
    private RestaurantSort.SortBy orderBy = RestaurantSort.SortBy.RESTAURANT_ID;

    @Schema(description = "排序方向")
    @Default
    private RestaurantSort.SortOrder sort = RestaurantSort.SortOrder.ASC;
}
