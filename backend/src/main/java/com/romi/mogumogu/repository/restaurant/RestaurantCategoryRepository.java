package com.romi.mogumogu.repository.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategoryEntity, Integer> {
    /** 檢查群組是否存在 */
    boolean existsByGroupId(Integer groupId);

    /** 統計群組內分類數量 */
    long countByGroupId(Integer groupId);

    /** 取得分類 */
    Optional<RestaurantCategoryEntity> findByCategoryIdAndGroupId(Integer categoryId, Integer groupId);

    /** 取得群組內所有分類（依顯示排序） */
    List<RestaurantCategoryEntity> findByGroupIdOrderByDisplayOrderIdAsc(Integer groupId);

    /** 取得群組內目前最大的 displayOrderId */
    RestaurantCategoryEntity findTopByGroupIdOrderByDisplayOrderIdDesc(Integer groupId);

    /** 檢查同群組內是否有重複分類名稱 */
    boolean existsByGroupIdAndCategoryNameAndCategoryIdNot(Integer groupId, String categoryName, Integer categoryId);
}
