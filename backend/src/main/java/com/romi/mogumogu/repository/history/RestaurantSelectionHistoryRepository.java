package com.romi.mogumogu.repository.history;

import com.romi.mogumogu.entity.history.RestaurantSelectionHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantSelectionHistoryRepository
                extends JpaRepository<RestaurantSelectionHistoryEntity, Integer>,
                JpaSpecificationExecutor<RestaurantSelectionHistoryEntity>,
                RestaurantSelectionHistoryRepositoryCustom {

        /** 取得餐廳抽選歷史紀錄 */
        @EntityGraph(attributePaths = { "restaurant", "restaurant.categoryId" })
        @Override
        Page<RestaurantSelectionHistoryEntity> findAll(
                        Specification<RestaurantSelectionHistoryEntity> spec, Pageable pageable);

        /** 刪除餐廳底下的所有選取歷史 */
        void deleteByRestaurant_RestaurantId(Integer restaurantId);

        /** 刪除群組底下的所有選取歷史 */
        void deleteByGroupId(Integer groupId);
}
