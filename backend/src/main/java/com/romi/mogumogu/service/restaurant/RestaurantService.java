package com.romi.mogumogu.service.restaurant;

import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
import com.romi.mogumogu.dto.UpdateRestaurantDto;
import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.enums.RestaurantSort;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantCategoryRepository restaurantCategoryRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantCategoryRepository = restaurantCategoryRepository;
    }

    /** 取得所有餐廳 */
    public List<RestaurantResponse> getRestaurants(GetRestaurantQuery queryParams) {
        Integer groupId = queryParams.getGroupId();
        Integer categoryId = queryParams.getCategoryId();
        Boolean isArchived = queryParams.getIsArchived();
        String search = queryParams.getSearch();
        RestaurantSort.SortBy orderBy = queryParams.getOrderBy();
        RestaurantSort.SortOrder sort = queryParams.getSort();

        String normalizedSearch = Optional.ofNullable(search)
                .map(String::trim) // 去除前後空白
                .filter(value -> !value.isEmpty()) // 去除空字串
                .map(value -> value.toLowerCase(Locale.ROOT)) // 轉換為小寫
                .orElse(null); // 如果為空則返回 null

        // 建立不帶任何過濾條件的查詢規格
        Specification<RestaurantEntity> spec = Specification.unrestricted();

        // 加入過濾條件
        spec = spec.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 檢查群組 ID
            if (groupId != null) {
                predicates.add(cb.equal(root.get("groupId"), groupId));
            }

            // 檢查分類 ID
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId").get("categoryId"), categoryId));
            }

            // 檢查是否已刪除
            if (isArchived != null) {
                predicates.add(cb.equal(root.get("isArchived"), isArchived));
            }

            // 檢查餐廳名稱
            if (normalizedSearch != null) {
                String keyword = "%" + normalizedSearch + "%";
                predicates.add(cb.like(cb.lower(root.get("restaurantName")), keyword));
            }

            // 回傳過濾條件
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        // 取得排序方式
        RestaurantSort.SortOrder safeSort = RestaurantSort.SortOrder.ASC;
        if (sort != null) {
            safeSort = sort;
        }

        // 取得排序欄位
        RestaurantSort.SortBy safeOrderBy = RestaurantSort.SortBy.DISPLAY_ORDER;
        if (orderBy != null) {
            safeOrderBy = orderBy;
        }

        // 建立排序規則
        Sort.Direction sortDirection = Objects.requireNonNull(safeSort.getSortDirection());

        // 取得排序欄位名稱
        String sortProperty = safeOrderBy.getSortProperty();

        // 建立排序規則
        Sort jpaSort = Sort.by(sortDirection, sortProperty);

        // 取得餐廳列表
        List<RestaurantEntity> entities = restaurantRepository.findAll(spec, jpaSort);
        List<RestaurantResponse> restaurantResponses = entities.stream()
                .map(RestaurantResponse::restaurantResponse)
                .toList();

        return restaurantResponses;
    }

    /** 新增餐廳 */
    public RestaurantResponse createRestaurant(CreateRestaurantDto request) {
        // 取得群組 ID
        Integer groupId = request.getGroupId();
        if (!restaurantCategoryRepository.existsByGroupId(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group not found");
        }

        // 取得分類
        RestaurantCategoryEntity category = findCategoryOrThrow(request.getCategoryId(), groupId);

        // 取得同群組內目前最大的 displayOrder
        RestaurantEntity latestRestaurant = restaurantRepository
                .findTopByGroupIdOrderByDisplayOrderDesc(groupId);
        Integer nextDisplayOrder;
        if (latestRestaurant == null || latestRestaurant.getDisplayOrder() == null) {
            nextDisplayOrder = 1;
        } else {
            nextDisplayOrder = latestRestaurant.getDisplayOrder() + 1;
        }

        // 新增餐廳
        Date now = new Date();
        RestaurantEntity entity = Objects.requireNonNull(RestaurantEntity.builder()
                .groupId(groupId)
                .categoryId(category)
                .displayOrder(nextDisplayOrder)
                .selectedCount(0)
                .restaurantName(request.getRestaurantName())
                .note(request.getNote())
                .imageUrl(request.getImageUrl())
                .isArchived(false)
                .lastSelectedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build());

        RestaurantEntity savedEntity = restaurantRepository.save(entity);
        return RestaurantResponse.restaurantResponse(savedEntity);
    }

    /** 修改餐廳 */
    public RestaurantResponse updateRestaurant(Integer restaurantId, UpdateRestaurantDto request) {
        // 檢查餐廳是否存在
        if (restaurantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restaurantId must not be null");
        }

        // 取得餐廳
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }

        // 修改分類
        if (request.getCategoryId() != null) {
            Integer targetGroupId = restaurant.getGroupId();
            Integer targetCategoryId = request.getCategoryId();
            RestaurantCategoryEntity category = findCategoryOrThrow(targetCategoryId, targetGroupId);
            restaurant.setCategoryId(category);
        }

        // 修改顯示順序
        if (request.getDisplayOrder() != null) {
            Integer displayOrder = request.getDisplayOrder();

            // 檢查顯示順序是否已存在
            if (restaurantRepository.existsByGroupIdAndDisplayOrderAndRestaurantIdNot(
                    restaurant.getGroupId(), displayOrder, restaurant.getRestaurantId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayOrder already exists in this group");
            }

            restaurant.setDisplayOrder(displayOrder);
        }

        // 修改選中次數
        if (request.getSelectedCount() != null) {
            restaurant.setSelectedCount(request.getSelectedCount());
        }

        // 修改餐廳名稱
        if (request.getRestaurantName() != null && !request.getRestaurantName().isBlank()) {
            String restaurantName = request.getRestaurantName();
            restaurant.setRestaurantName(restaurantName);
        }

        // 修改備註
        if (request.getNote() != null) {
            restaurant.setNote(request.getNote());
        }

        // 修改圖片 URL
        if (request.getImageUrl() != null) {
            restaurant.setImageUrl(request.getImageUrl());
        }

        // 修改最後選中時間
        if (request.getLastSelectedAt() != null) {
            restaurant.setLastSelectedAt(request.getLastSelectedAt());
        }

        // 更新最後修改時間
        restaurant.setUpdatedAt(new Date());

        // 寫回資料庫並轉成回傳 DTO
        RestaurantEntity updatedEntity = restaurantRepository.save(restaurant);
        return RestaurantResponse.restaurantResponse(updatedEntity);
    }

    /** 軟刪除餐廳 */
    public RestaurantResponse deleteRestaurant(Integer restaurantId) {
        // 檢查餐廳是否存在
        if (restaurantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restaurantId must not be null");
        }

        // 取得餐廳
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }

        restaurant.setIsArchived(true);
        restaurant.setUpdatedAt(new Date());
        RestaurantEntity deletedEntity = restaurantRepository.save(restaurant);
        return RestaurantResponse.restaurantResponse(deletedEntity);
    }

    /** 檢查分類是否存在 */
    private RestaurantCategoryEntity findCategoryOrThrow(Integer categoryId, Integer groupId) {
        return restaurantCategoryRepository
                .findByCategoryIdAndGroupId(categoryId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

}
