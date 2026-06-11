package com.romi.mogumogu.repository.dish;

import com.romi.mogumogu.entity.dish.DishEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<DishEntity, Integer> {
    /** 取得餐廳內目前最大的 displayOrderId */
    DishEntity findTopByRestaurantId_RestaurantIdOrderByDisplayOrderIdDesc(Integer restaurantId);

    /** 取得餐廳內所有餐點 */
    List<DishEntity> findByRestaurantId_RestaurantIdOrderByDisplayOrderIdAsc(Integer restaurantId);

    /** 檢查同一餐廳內是否已有其他餐點使用此 displayOrderId */
    boolean existsByRestaurantId_RestaurantIdAndDisplayOrderIdAndDishIdNot(
            Integer restaurantId, Integer displayOrderId, Integer dishId);
}
