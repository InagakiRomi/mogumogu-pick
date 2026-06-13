package com.romi.mogumogu.service.restaurant;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.Response.RestaurantCategoryResponse;
import com.romi.mogumogu.dto.CreateRestaurantCategoryDto;
import com.romi.mogumogu.dto.UpdateRestaurantCategoryDto;
import com.romi.mogumogu.entity.restaurant.RestaurantCategoryEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import com.romi.mogumogu.security.SecurityUtils;

@Service
public class RestaurantCategoryService {

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of("主食", "輕食", "飲料");

    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantCategoryService(
            RestaurantCategoryRepository restaurantCategoryRepository,
            RestaurantRepository restaurantRepository,
            UserRepository userRepository) {
        this.restaurantCategoryRepository = restaurantCategoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    /** 取得目前登入使用者所屬群組的分類清單 */
    public List<RestaurantCategoryResponse> getMyGroupCategories() {
        Integer groupId = resolveCurrentUserGroupId();
        return restaurantCategoryRepository.findByGroupIdOrderByDisplayOrderIdAsc(groupId).stream()
                .map(entity -> toResponse(groupId, entity))
                .toList();
    }

    /** 新增分類 */
    @Transactional
    public RestaurantCategoryResponse createCategory(CreateRestaurantCategoryDto request) {
        Integer groupId = resolveCurrentUserGroupId();
        String categoryName = normalizeCategoryName(request.getCategoryName());

        // 檢查同群組內是否有重複分類名稱
        if (restaurantCategoryRepository.existsByGroupIdAndCategoryNameAndCategoryIdNot(
                groupId, categoryName, -1)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        // 新增分類
        RestaurantCategoryEntity latest = restaurantCategoryRepository
                .findTopByGroupIdOrderByDisplayOrderIdDesc(groupId);
        int displayOrderId = (latest == null || latest.getDisplayOrderId() == null)
                ? 1
                : latest.getDisplayOrderId() + 1;

        Date now = new Date();
        RestaurantCategoryEntity entity = Objects.requireNonNull(RestaurantCategoryEntity.builder()
                .groupId(groupId)
                .displayOrderId(displayOrderId)
                .categoryName(categoryName)
                .createdAt(now)
                .build());

        RestaurantCategoryEntity saved = restaurantCategoryRepository.save(entity);
        return toResponse(groupId, saved);
    }

    /** 修改分類 */
    @Transactional
    public RestaurantCategoryResponse updateCategory(Integer categoryId, UpdateRestaurantCategoryDto request) {
        Integer groupId = resolveCurrentUserGroupId();
        RestaurantCategoryEntity category = findCategoryOrThrow(categoryId, groupId);

        // 檢查是否有要更新的欄位
        String categoryName = request.getCategoryName();
        Integer displayOrderId = request.getDisplayOrderId();
        if ((categoryName == null || categoryName.isBlank()) && displayOrderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        // 修改分類名稱
        if (categoryName != null && !categoryName.isBlank()) {
            String normalizedName = normalizeCategoryName(categoryName);
            if (restaurantCategoryRepository.existsByGroupIdAndCategoryNameAndCategoryIdNot(
                    groupId, normalizedName, categoryId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
            }
            category.setCategoryName(normalizedName);
        }

        // 修改顯示排序
        if (displayOrderId != null) {
            category.setDisplayOrderId(displayOrderId);
        }

        RestaurantCategoryEntity saved = Objects.requireNonNull(
                restaurantCategoryRepository.save(Objects.requireNonNull(category)));
        return toResponse(groupId, saved);
    }

    /** 刪除分類 */
    @Transactional
    public void deleteCategory(Integer categoryId) {
        Integer groupId = resolveCurrentUserGroupId();
        RestaurantCategoryEntity category = findCategoryOrThrow(categoryId, groupId);

        // 檢查是否為最後一個分類
        if (restaurantCategoryRepository.countByGroupId(groupId) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last category");
        }

        // 檢查是否有餐廳使用此分類
        if (restaurantRepository.existsByGroupIdAndCategoryId_CategoryId(groupId, categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is in use by restaurants");
        }

        restaurantCategoryRepository.delete(Objects.requireNonNull(category));
    }

    /** 為群組種入預設分類 */
    @Transactional
    public void seedDefaultCategories(Integer groupId) {
        Date now = new Date();
        for (int i = 0; i < DEFAULT_CATEGORY_NAMES.size(); i++) {
            RestaurantCategoryEntity entity = Objects.requireNonNull(RestaurantCategoryEntity.builder()
                    .groupId(groupId)
                    .displayOrderId(i + 1)
                    .categoryName(DEFAULT_CATEGORY_NAMES.get(i))
                    .createdAt(now)
                    .build());
            restaurantCategoryRepository.save(entity);
        }
    }

    /** 將分類實體轉換為分類回應 */
    private RestaurantCategoryResponse toResponse(Integer groupId, RestaurantCategoryEntity entity) {
        long restaurantCount = restaurantRepository
                .countByGroupIdAndCategoryId_CategoryId(groupId, entity.getCategoryId());
        return RestaurantCategoryResponse.from(entity, restaurantCount);
    }

    /** 正規化分類名稱 */
    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryName is required");
        }
        String normalized = categoryName.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryName is required");
        }
        return normalized;
    }

    /** 取得分類 */
    private RestaurantCategoryEntity findCategoryOrThrow(Integer categoryId, Integer groupId) {
        return restaurantCategoryRepository
                .findByCategoryIdAndGroupId(categoryId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    /** 取得目前登入使用者所屬群組 ID */
    private Integer resolveCurrentUserGroupId() {
        Integer userId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        UserEntity user = userOpt.get();
        if (user.getGroupId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group");
        }
        return user.getGroupId();
    }
}
