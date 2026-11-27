package com.romi.mogumogu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.romi.mogumogu.entity.RestaurantEntity;

public interface RestaurantRepository extends
                JpaRepository<RestaurantEntity, Integer>,
                JpaSpecificationExecutor<RestaurantEntity> {
        /** 檢查群組內是否有餐廳使用指定分類 */
        boolean existsByGroupIdAndCategoryId(Integer groupId, Integer categoryId);

        /** 檢查同群組內是否有重複排序ID，且略過自己 */
        boolean existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(
                        Integer groupId, Integer displayOrderId, Integer restaurantId);

        /** 取得群組內目前最大的displayOrderId */
        @Query("""
                        SELECT COALESCE(MAX(r.displayOrderId), 0)
                        FROM RestaurantEntity r
                        WHERE r.groupId = :groupId
                        """)
        Integer findMaxDisplayOrderIdByGroupId(@Param("groupId") Integer groupId);
}
