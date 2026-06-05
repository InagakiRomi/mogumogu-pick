package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository
        extends JpaRepository<RestaurantEntity, Integer>, JpaSpecificationExecutor<RestaurantEntity> {
    /** 取得群組內目前最大的 displayOrderId */
    RestaurantEntity findTopByGroupIdOrderByDisplayOrderIdDesc(Integer groupId);

    /** 檢查同群組內是否有重複排序 */
    boolean existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(Integer groupId, Integer displayOrderId,
            Integer restaurantId);

    /** 取得群組內所有未封存餐廳 */
    List<RestaurantEntity> findByGroupIdAndIsArchivedFalse(Integer groupId);

    /** 取得群組內指定分類的未封存餐廳 */
    List<RestaurantEntity> findByGroupIdAndCategoryId_CategoryIdAndIsArchivedFalse(Integer groupId, Integer categoryId);

    /** 取得群組內指定未封存餐廳 */
    Optional<RestaurantEntity> findByRestaurantIdAndGroupIdAndIsArchivedFalse(
            Integer restaurantId, Integer groupId);
}
