package com.romi.mogumogu.repository.history;

import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantSelectionHistoryRepository
                extends JpaRepository<RestaurantSelectionHistoryEntity, Integer> {
        /** 查詢群組的餐廳選擇歷史，並按選擇時間排序 */
        @EntityGraph(attributePaths = { "restaurant", "restaurant.categoryId" })
        Page<RestaurantSelectionHistoryEntity> findByGroupIdOrderBySelectedAtDesc(
                        Integer groupId, Pageable pageable);
}
