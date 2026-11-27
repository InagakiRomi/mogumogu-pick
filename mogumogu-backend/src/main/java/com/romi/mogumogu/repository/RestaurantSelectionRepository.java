package com.romi.mogumogu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.romi.mogumogu.entity.RestaurantSelectionEntity;

public interface RestaurantSelectionRepository extends JpaRepository<RestaurantSelectionEntity, Integer> {
        /** 取得指定群組的選擇歷史 */
        Page<RestaurantSelectionEntity> findByGroupIdOrderBySelectedAtDesc(Integer groupId, Pageable pageable);

        /** 刪除某餐廳底下所有選擇紀錄 */
        @Modifying
        @Query("DELETE FROM RestaurantSelectionEntity r WHERE r.restaurant.restaurantId = :restaurantId")
        int deleteAllByRestaurantId(Integer restaurantId);
}