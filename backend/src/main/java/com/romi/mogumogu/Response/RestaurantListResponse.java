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
public class RestaurantListResponse {
    /** 餐廳列表 */
    private List<RestaurantResponse> data;
    /** 頁數 */
    private Integer page;
    /** 每頁筆數 */
    private Integer limit;
    /** 總筆數 */
    private Long total;
}
