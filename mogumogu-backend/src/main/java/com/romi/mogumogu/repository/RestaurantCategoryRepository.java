package com.romi.mogumogu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.romi.mogumogu.entity.RestaurantCategoryEntity;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategoryEntity, Integer> {
    /** 依id取得分類 */
    Page<RestaurantCategoryEntity> findByGroupId(Integer groupId, Pageable pageable);

    /** 檢查同群組內是否有重複分類名稱 */
    boolean existsByGroupIdAndCategoryName(Integer groupId, String categoryName);

    /** 檢查同群組內是否有重複分類名稱，且略過自己 */
    boolean existsByGroupIdAndCategoryNameAndCategoryIdNot(Integer groupId, String categoryName,
            Integer categoryId);

    /** 檢查同群組內是否有重複排序ID，且略過自己 */
    boolean existsByGroupIdAndDisplayOrderIdAndCategoryIdNot(Integer groupId, Integer displayOrderId,
            Integer categoryId);

    /** 取得群組內目前最大的displayOrderId */
    @Query("""
            SELECT COALESCE(MAX(r.displayOrderId), 0)
            FROM RestaurantCategoryEntity r
            WHERE r.groupId = :groupId
            """)
    Integer findMaxDisplayOrderIdByGroupId(@Param("groupId") Integer groupId);

    /** 統計群組內分類數量 */
    Integer countByGroupId(Integer groupId);
}
