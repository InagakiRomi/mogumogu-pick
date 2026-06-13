package com.romi.mogumogu.repository.history;

import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface RestaurantSelectionHistoryRepository
        extends JpaRepository<RestaurantSelectionHistoryEntity, Integer>,
        JpaSpecificationExecutor<RestaurantSelectionHistoryEntity> {

    /** 取得餐廳抽選歷史紀錄 */
    @EntityGraph(attributePaths = { "restaurant", "restaurant.categoryId" })
    @Override
    @NonNull
    Page<RestaurantSelectionHistoryEntity> findAll(
            @Nullable Specification<RestaurantSelectionHistoryEntity> spec, @NonNull Pageable pageable);

    /** 刪除餐廳底下的所有選取歷史 */
    void deleteByRestaurant_RestaurantId(Integer restaurantId);
}
