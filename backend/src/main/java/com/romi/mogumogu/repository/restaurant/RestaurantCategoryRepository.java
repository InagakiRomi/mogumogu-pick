package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategoryEntity, Integer> {
    /** 檢查群組是否存在 */
    boolean existsByGroupId(Integer groupId);

    /** 取得分類 */
    Optional<RestaurantCategoryEntity> findByCategoryIdAndGroupId(Integer categoryId, Integer groupId);
}
