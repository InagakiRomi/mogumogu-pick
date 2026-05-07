package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Integer> {
    /** 取得群組內目前最大的 displayOrder */
    RestaurantEntity findTopByGroupIdOrderByDisplayOrderDesc(Integer groupId);

    /** 檢查同群組內是否有重複排序 */
    boolean existsByGroupIdAndDisplayOrderAndRestaurantIdNot(Integer groupId, Integer displayOrder,
            Integer restaurantId);
}
