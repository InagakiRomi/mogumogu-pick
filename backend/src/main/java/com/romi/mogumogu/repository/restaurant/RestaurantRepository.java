package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantRepository
        extends JpaRepository<RestaurantEntity, Integer>, JpaSpecificationExecutor<RestaurantEntity> {
    /** 取得群組內目前最大的 displayOrderId */
    RestaurantEntity findTopByGroupIdOrderByDisplayOrderIdDesc(Integer groupId);

    /** 檢查同群組內是否有重複排序 */
    boolean existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(Integer groupId, Integer displayOrderId,
            Integer restaurantId);
}
