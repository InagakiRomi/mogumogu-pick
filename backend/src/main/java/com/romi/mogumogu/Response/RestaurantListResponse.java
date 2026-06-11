package com.romi.mogumogu.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantListResponse<T> {
    /** 資料列表 */
    private List<T> data;

    /** 頁碼 */
    private Integer page;

    /** 每頁筆數 */
    private Integer limit;

    /** 總筆數 */
    private Long total;

    /** 分類清單 */
    private List<RestaurantCategoryResponse> categories;

    public static <T> RestaurantListResponse<T> of(List<T> data, int page, int limit, long total) {
        return RestaurantListResponse.<T>builder()
                .data(data)
                .page(page)
                .limit(limit)
                .total(total)
                .build();
    }

    public static <T> RestaurantListResponse<T> of(
            List<T> data,
            int page,
            int limit,
            long total,
            List<RestaurantCategoryResponse> categories) {
        return RestaurantListResponse.<T>builder()
                .data(data)
                .page(page)
                .limit(limit)
                .total(total)
                .categories(categories)
                .build();
    }
}
