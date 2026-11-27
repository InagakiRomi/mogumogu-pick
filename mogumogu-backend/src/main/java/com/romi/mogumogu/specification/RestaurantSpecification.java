package com.romi.mogumogu.specification;

import org.springframework.data.jpa.domain.Specification;
import com.romi.mogumogu.entity.RestaurantEntity;

public class RestaurantSpecification {
    /** 所屬群組查詢 */
    public static Specification<RestaurantEntity> hasGroupId(Integer groupId) {
        return (root, query, cb) -> groupId == null
                ? null
                : cb.equal(root.get("groupId"), groupId);
    }

    /** 餐廳名稱模糊查詢 */
    public static Specification<RestaurantEntity> hasKeyword(String keyword) {
        return (root, query, cb) -> keyword == null || keyword.isBlank()
                ? null
                : cb.like(root.get("restaurantName"), "%" + keyword + "%");
    }

    /** 餐廳分類查詢 */
    public static Specification<RestaurantEntity> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("categoryId"), categoryId);
    }
}