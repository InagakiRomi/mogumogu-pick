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
public class DishListResponse {
    /** 餐點列表 */
    private List<DishResponse> data;
    /** 總筆數 */
    private Integer total;
}
