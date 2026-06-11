package com.romi.mogumogu.service.restaurant;

import com.romi.mogumogu.Response.DishListResponse;
import com.romi.mogumogu.Response.RestaurantCategoryResponse;
import com.romi.mogumogu.Response.RestaurantListResponse;
import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.Response.SelectionHistoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.dto.GetRestaurantQuery;
import com.romi.mogumogu.dto.GetSelectionHistoryQuery;
import com.romi.mogumogu.dto.UpdateRestaurantDto;
import com.romi.mogumogu.service.dish.DishService;
import com.romi.mogumogu.service.history.RestaurantSelectionHistoryService;
import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.RestaurantSort;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import com.romi.mogumogu.logging.JulLoggerFactory;
import com.romi.mogumogu.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@Service
public class RestaurantService {
    private static final Logger renderLog = new JulLoggerFactory().printRenderLog();

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final UserRepository userRepository;
    private final RestaurantSelectionHistoryService selectionHistoryService;
    private final DishService dishService;
    private final Map<Integer, RandomPool> randomPoolByUser = new ConcurrentHashMap<>();

    private record RandomPool(Integer categoryId, List<Integer> restaurantIds, int totalCount) {
    }

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantCategoryRepository restaurantCategoryRepository,
            UserRepository userRepository,
            RestaurantSelectionHistoryService restaurantSelectionHistoryService,
            DishService dishService) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantCategoryRepository = restaurantCategoryRepository;
        this.userRepository = userRepository;
        this.selectionHistoryService = restaurantSelectionHistoryService;
        this.dishService = dishService;
    }

    /** 取得餐廳清單（可依群組、分類、封存狀態等條件篩選） */
    public RestaurantListResponse<RestaurantResponse> getRestaurants(GetRestaurantQuery queryParams) {
        if (Boolean.TRUE.equals(queryParams.getMine())) {
            queryParams.setGroupId(resolveCurrentUserGroupId());
            if (queryParams.getIsArchived() == null) {
                queryParams.setIsArchived(false);
            }
        }

        Integer groupId = queryParams.getGroupId();
        Integer categoryId = queryParams.getCategoryId();
        Boolean isArchived = queryParams.getIsArchived();
        String search = queryParams.getSearch();
        RestaurantSort.SortBy orderBy = queryParams.getOrderBy();
        RestaurantSort.SortOrder sort = queryParams.getSort();
        Integer page = queryParams.getPage();
        Integer limit = queryParams.getLimit();

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
        RestaurantSort.SortBy safeOrderBy = RestaurantSort.SortBy.DISPLAY_ORDER_ID;
        if (orderBy != null) {
            safeOrderBy = orderBy;
        }

        // 建立排序規則
        Sort.Direction sortDirection = Objects.requireNonNull(safeSort.getSortDirection());

        // 取得排序欄位名稱
        String sortProperty = safeOrderBy.getSortProperty();

        // 建立排序規則
        Sort jpaSort = Sort.by(sortDirection, sortProperty);

        // JPA Pageable 使用 0-based，API 採 1-based
        Pageable pageable = PageRequest.of(page - 1, limit, jpaSort);

        // 取得餐廳列表
        Page<RestaurantEntity> pageResult = restaurantRepository.findAll(spec, pageable);
        List<RestaurantResponse> restaurantResponses = pageResult.getContent().stream()
                .map(RestaurantResponse::restaurantResponse)
                .toList();

        if (Boolean.TRUE.equals(queryParams.getIncludeCategories()) && groupId != null) {
            List<RestaurantCategoryResponse> categories = findRestaurantCategories(groupId);
            return RestaurantListResponse.of(
                    restaurantResponses, page, limit, pageResult.getTotalElements(), categories);
        }

        return RestaurantListResponse.of(restaurantResponses, page, limit, pageResult.getTotalElements());
    }

    /** 依餐廳 ID 取得目前登入使用者所屬群組的單筆餐廳資訊（不含已封存） */
    public RestaurantResponse getRestaurant(Integer restaurantId, boolean includeDishes) {
        Integer groupId = resolveCurrentUserGroupId();
        RestaurantEntity restaurant = findActiveRestaurantOrThrow(restaurantId, groupId);
        RestaurantResponse response = RestaurantResponse.restaurantResponse(restaurant);
        if (includeDishes) {
            DishListResponse dishList = dishService.getRestaurantDishes(restaurantId);
            response.setDishes(dishList.getData());
            response.setDishTotal(dishList.getTotal());
        }
        return response;
    }

    /** 抽取目前登入使用者所屬群組的一間餐廳 */
    public RestaurantResponse getRandomMyGroupRestaurant(Integer categoryId) {
        // 取得目前登入使用者的 ID
        Integer userId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());

        // 取得目前登入使用者的群組 ID
        Integer groupId = resolveCurrentUserGroupId();

        // 先從抽籤池取得餐廳 ID 清單，沒有資料或切換分類才重新查詢
        RandomPool randomPool = randomPoolByUser.get(userId);
        if (randomPool == null
                || !Objects.equals(randomPool.categoryId(), categoryId)
                || randomPool.restaurantIds().isEmpty()) {
            List<RestaurantEntity> restaurants;
            if (categoryId == null) {
                restaurants = restaurantRepository.findByGroupIdAndIsArchivedFalse(groupId);
            } else {
                restaurants = restaurantRepository.findByGroupIdAndCategoryId_CategoryIdAndIsArchivedFalse(groupId,
                        categoryId);
            }

            // 檢查餐廳列表是否為空
            if (restaurants.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No available restaurants found for this filter");
            }

            // 將餐廳 ID 放進抽籤池
            List<Integer> restaurantIds = new ArrayList<>();
            for (RestaurantEntity restaurant : restaurants) {
                restaurantIds.add(restaurant.getRestaurantId());
            }
            randomPool = new RandomPool(categoryId, restaurantIds, restaurantIds.size());
            randomPoolByUser.put(userId, randomPool);
        }

        // 取得抽籤池中的餐廨 ID 清單
        List<Integer> pool = randomPool.restaurantIds();

        // 取得抽籤池中的餐廨總數量
        int totalCount = randomPool.totalCount();

        // 隨機取得一筆餐廨 ID
        int randomIndex = ThreadLocalRandom.current().nextInt(pool.size());
        Integer restaurantId = pool.remove(randomIndex);
        int drawnCount = totalCount - pool.size();

        // 檢查抽籤池是否為空，為空則移除抽籤池
        if (pool.isEmpty()) {
            clearMyGroupRandomPool();
        }

        // 取得選中的餐廳
        RestaurantEntity selectedRestaurant = findRestaurantOrThrow(restaurantId);

        // 記錄抽籤結果
        renderLog.info(String.format(
                "Restaurant pool total: %d, drawn so far: %d",
                totalCount, drawnCount));
        return RestaurantResponse.restaurantResponse(selectedRestaurant);
    }

    /** 取得自己所屬群組的餐廳抽選歷史紀錄 */
    public RestaurantListResponse<SelectionHistoryResponse> getMyGroupSelectionHistory(
            GetSelectionHistoryQuery queryParams) {
        return selectionHistoryService.getMyGroupSelectionHistory(queryParams);
    }

    /** 確認選擇餐廳，更新選取紀錄並重置抽籤池 */
    @Transactional
    public RestaurantResponse chooseMyGroupRestaurant(Integer restaurantId) {
        // 取得目前登入使用者的群組 ID
        Integer groupId = resolveCurrentUserGroupId();

        // 檢查餐廳 ID 是否存在、屬於指定群組且未被封存
        RestaurantEntity restaurant = findActiveRestaurantOrThrow(restaurantId, groupId);

        // 重置目前使用者的抽籤池
        clearMyGroupRandomPool();

        // 更新選取紀錄
        Date now = new Date();
        restaurant.setSelectedCount(restaurant.getSelectedCount() + 1);
        restaurant.setLastSelectedAt(now);
        restaurant.setUpdatedAt(now);

        RestaurantEntity savedRestaurant = restaurantRepository.save(restaurant);
        selectionHistoryService.recordSelection(groupId, savedRestaurant, now);

        return RestaurantResponse.restaurantResponse(savedRestaurant);
    }

    /** 重置目前登入使用者的抽籤池 */
    public void clearMyGroupRandomPool() {
        Integer userId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());
        if (randomPoolByUser.remove(userId) != null) {
            renderLog.info("Restaurant pool has been reset");
        }
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

        // 取得同群組內目前最大的 displayOrderId
        RestaurantEntity latestRestaurant = restaurantRepository
                .findTopByGroupIdOrderByDisplayOrderIdDesc(groupId);
        Integer nextDisplayOrderId;
        if (latestRestaurant == null || latestRestaurant.getDisplayOrderId() == null) {
            nextDisplayOrderId = 1;
        } else {
            nextDisplayOrderId = latestRestaurant.getDisplayOrderId() + 1;
        }

        // 新增餐廳
        Date now = new Date();
        RestaurantEntity entity = Objects.requireNonNull(RestaurantEntity.builder()
                .groupId(groupId)
                .categoryId(category)
                .displayOrderId(nextDisplayOrderId)
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
        // 檢查餐廳 ID 是否存在
        RestaurantEntity restaurant = findRestaurantOrThrow(restaurantId);

        // 修改分類
        if (request.getCategoryId() != null) {
            Integer targetGroupId = restaurant.getGroupId();
            Integer targetCategoryId = request.getCategoryId();
            RestaurantCategoryEntity category = findCategoryOrThrow(targetCategoryId, targetGroupId);
            restaurant.setCategoryId(category);
        }

        // 修改顯示順序
        if (request.getDisplayOrderId() != null) {
            Integer displayOrderId = request.getDisplayOrderId();

            // 檢查顯示順序是否已存在
            if (restaurantRepository.existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(
                    restaurant.getGroupId(), displayOrderId, restaurant.getRestaurantId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "displayOrderId already exists in this group");
            }

            restaurant.setDisplayOrderId(displayOrderId);
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
        // 檢查餐廳 ID 是否存在
        RestaurantEntity restaurant = findRestaurantOrThrow(restaurantId);

        restaurant.setIsArchived(true);
        restaurant.setUpdatedAt(new Date());
        RestaurantEntity deletedEntity = restaurantRepository.save(restaurant);
        return RestaurantResponse.restaurantResponse(deletedEntity);
    }

    /** 取得目前登入使用者的群組 ID */
    private Integer resolveCurrentUserGroupId() {
        // 取得目前登入使用者的 ID
        Integer userId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        UserEntity user = userOpt.get();

        // 檢查使用者是否屬於群組
        if (user.getGroupId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group");
        }
        return user.getGroupId();
    }

    /** 檢查餐廳是否存在且屬於指定群組 */
    private RestaurantEntity findRestaurantInGroupOrThrow(Integer restaurantId, Integer groupId) {
        RestaurantEntity restaurant = findRestaurantOrThrow(restaurantId);

        if (!Objects.equals(restaurant.getGroupId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Restaurant belongs to another group");
        }
        return restaurant;
    }

    /** 檢查餐廳是否存在、屬於指定群組且未被封存 */
    private RestaurantEntity findActiveRestaurantOrThrow(Integer restaurantId, Integer groupId) {
        RestaurantEntity restaurant = findRestaurantInGroupOrThrow(restaurantId, groupId);

        if (Boolean.TRUE.equals(restaurant.getIsArchived())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Restaurant is archived");
        }
        return restaurant;
    }

    /** 檢查餐廳 ID 參數並取得餐廳實體 */
    private RestaurantEntity findRestaurantOrThrow(Integer restaurantId) {
        // 檢查餐廳 ID 是否為 null
        if (restaurantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restaurantId must not be null");
        }

        // 取得餐廳
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }

        return restaurant;
    }

    /** 取得群組內所有分類（依顯示排序） */
    private List<RestaurantCategoryResponse> findRestaurantCategories(Integer groupId) {
        return restaurantCategoryRepository.findByGroupIdOrderByDisplayOrderIdAsc(groupId).stream()
                .map(RestaurantCategoryResponse::from)
                .toList();
    }

    /** 檢查分類是否存在 */
    private RestaurantCategoryEntity findCategoryOrThrow(Integer categoryId, Integer groupId) {
        return restaurantCategoryRepository
                .findByCategoryIdAndGroupId(categoryId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

}
