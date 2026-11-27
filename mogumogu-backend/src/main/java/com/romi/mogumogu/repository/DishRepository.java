
package com.romi.mogumogu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.romi.mogumogu.entity.DishEntity;

public interface DishRepository extends JpaRepository<DishEntity, Integer> {
    /** 取得餐廳內所有餐點 */
    Page<DishEntity> findByRestaurantIdOrderByDisplayOrderIdAsc(Integer restaurantId, Pageable pageable);

    /** 檢查餐廳內是否有重複餐點名稱 */
    boolean existsByRestaurantIdAndDishName(Integer restaurantId, String dishName);

    /** 檢查餐廳內是否有重複餐點名稱，且略過自己 */
    boolean existsByRestaurantIdAndDishNameAndDishIdNot(Integer restaurantId, String dishName, Integer dishId);

    /** 刪除某餐廳底下所有餐點 */
    @Modifying
    @Query("DELETE FROM DishEntity d WHERE d.restaurantId = :restaurantId")
    int deleteAllByRestaurantId(Integer restaurantId);

    /** 取得餐廳內目前最大的displayOrderId */
    @Query("""
            SELECT COALESCE(MAX(d.displayOrderId), 0)
            FROM DishEntity d
            WHERE d.restaurantId = :restaurantId
            """)
    Integer findMaxDisplayOrderIdByRestaurantId(@Param("restaurantId") Integer restaurantId);
}
