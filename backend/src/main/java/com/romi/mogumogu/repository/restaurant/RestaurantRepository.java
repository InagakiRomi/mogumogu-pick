package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RestaurantRepository
        extends JpaRepository<RestaurantEntity, Integer>, JpaSpecificationExecutor<RestaurantEntity> {
    /** 取得群組內目前最大的 displayOrderId */
    RestaurantEntity findTopByGroupIdOrderByDisplayOrderIdDesc(Integer groupId);

    /** 檢查同群組內是否有重複排序 */
    boolean existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(Integer groupId, Integer displayOrderId,
            Integer restaurantId);

    /** 取得群組內所有餐廳 */
    List<RestaurantEntity> findByGroupId(Integer groupId);

    /** 取得群組內指定分類的餐廳 */
    List<RestaurantEntity> findByGroupIdAndCategoryId_CategoryId(Integer groupId, Integer categoryId);

    /** 檢查群組內是否有餐廳使用指定分類 */
    boolean existsByGroupIdAndCategoryId_CategoryId(Integer groupId, Integer categoryId);

    /** 統計群組內使用指定分類的餐廳數量 */
    long countByGroupIdAndCategoryId_CategoryId(Integer groupId, Integer categoryId);
}
