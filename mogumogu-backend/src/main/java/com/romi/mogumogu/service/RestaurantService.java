package com.romi.mogumogu.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCreateRequest;
import com.romi.mogumogu.dto.restaurant.RestaurantResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantUpdateRequest;
import com.romi.mogumogu.entity.RestaurantEntity;
import com.romi.mogumogu.helper.UserHelper;
import com.romi.mogumogu.repository.RestaurantRepository;
import com.romi.mogumogu.specification.RestaurantSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final DishService dishService;
    private final RestaurantSelectionService restaurantSelectionService;
    private final UserHelper userHelper;

    /** 取得餐廳清單 */
    public PageResponse<RestaurantResponse> getRestaurants(
            Integer groupId, String keyword, Integer categoryId, Pageable pageable) {

        // 取得篩選條件
        Specification<RestaurantEntity> spec = Specification.allOf(
                RestaurantSpecification.hasGroupId(groupId),
                RestaurantSpecification.hasKeyword(keyword),
                RestaurantSpecification.hasCategoryId(categoryId));

        // 取得餐廳分頁資料
        Page<RestaurantEntity> restaurant = restaurantRepository.findAll(spec, pageable);

        // 將Entity轉換成Response DTO
        Page<RestaurantResponse> response = restaurant.map(RestaurantResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 依餐廳ID取得餐廳資料 */
    public RestaurantResponse getRestaurantById(Integer restaurantId) {
        // 尋找餐廳
        RestaurantEntity restaurant = getRestaurant(restaurantId);

        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 回傳群組資料
        return RestaurantResponse.from(restaurant);
    }

    /** 取得自己所屬群組的餐廳清單 */
    public PageResponse<RestaurantResponse> getRestaurantsByGroupId(
            String keyword, Integer categoryId, Pageable pageable) {
        // 取得目前登入帳號群組id
        Integer userGroupId = userHelper.getUserGroupId();

        return getRestaurants(userGroupId, keyword, categoryId, pageable);
    }

    /** 新增餐廳 */
    public RestaurantResponse createRestaurant(RestaurantCreateRequest restaurantCreateRequest) {
        // 建立資料
        Integer categoryId = restaurantCreateRequest.categoryId();
        String restaurantName = restaurantCreateRequest.restaurantName();
        String address = restaurantCreateRequest.address().trim();
        String note = restaurantCreateRequest.note().trim();
        String imageUrl = restaurantCreateRequest.imageUrl().trim();

        // 取得目前登入使用者群組id
        Integer groupId = userHelper.getUserGroupId();

        // 檢查分類是否存在
        validateCategory(groupId, categoryId);

        // 計算群組內最大排序
        Integer displayOrderId = restaurantRepository.findMaxDisplayOrderIdByGroupId(groupId) + 1;

        RestaurantEntity restaurant = new RestaurantEntity();

        restaurant.setGroupId(groupId);
        restaurant.setCategoryId(categoryId);
        restaurant.setDisplayOrderId(displayOrderId);
        restaurant.setSelectedCount(0);
        restaurant.setRestaurantName(restaurantName);
        restaurant.setAddress(address);
        restaurant.setNote(note);
        restaurant.setImageUrl(imageUrl);
        restaurant.setLastSelectedAt(null);

        // 儲存至資料庫
        RestaurantEntity savedRestaurant = restaurantRepository.save(restaurant);
        return RestaurantResponse.from(savedRestaurant);
    }

    /** 修改餐廳 */
    public RestaurantResponse updateRestaurant(Integer restaurantId, RestaurantUpdateRequest restaurantUpdateRequest) {
        // 取得資料
        Integer categoryId = restaurantUpdateRequest.categoryId();
        Integer displayOrderId = restaurantUpdateRequest.displayOrderId();
        Integer selectedCount = restaurantUpdateRequest.selectedCount();
        String restaurantName = restaurantUpdateRequest.restaurantName();
        String address = restaurantUpdateRequest.address().trim();
        String note = restaurantUpdateRequest.note().trim();
        String imageUrl = restaurantUpdateRequest.imageUrl().trim();
        LocalDateTime lastSelectedAt = restaurantUpdateRequest.lastSelectedAt();

        // 尋找餐廳
        RestaurantEntity restaurant = getRestaurant(restaurantId);
        Integer groudId = restaurant.getGroupId();

        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(groudId);

        // 修改分類
        if (categoryId != null) {
            // 檢查分類是否存在
            validateCategory(groudId, categoryId);
            restaurant.setCategoryId(categoryId);
        }

        // 修改顯示順序
        if (displayOrderId != null) {
            // 檢查顯示順序是否已存在
            if (restaurantRepository.existsByGroupIdAndDisplayOrderIdAndRestaurantIdNot(
                    groudId, displayOrderId, restaurantId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "群組內已經有排序編號" + displayOrderId);
            }

            restaurant.setDisplayOrderId(displayOrderId);
        }

        // 修改選中次數
        if (selectedCount != null) {
            restaurant.setSelectedCount(selectedCount);
        }

        // 修改餐廳名稱
        if (restaurantName != null && !restaurantName.isBlank()) {
            restaurant.setRestaurantName(restaurantName);
        }

        // 修改地址
        if (address != null && !address.isBlank()) {
            restaurant.setAddress(address);
        }

        // 修改備註
        if (note != null && !note.isBlank()) {
            restaurant.setNote(note);
        }

        // 修改圖片URL
        if (imageUrl != null && !imageUrl.isBlank()) {
            restaurant.setImageUrl(imageUrl);
        }

        // 修改最後選擇時間
        if (lastSelectedAt != null) {
            restaurant.setLastSelectedAt(lastSelectedAt);
        }

        // 儲存新的餐廳資料
        RestaurantEntity saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.from(saved);
    }

    /** 刪除餐廳 */
    public void deleteRestaurant(Integer restaurantId) {
        // 取得餐廳資料
        RestaurantEntity restaurant = getRestaurant(restaurantId);

        // 檢查餐廳是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 刪除餐廳底下所有的餐點
        dishService.deleteAllDishesByRestaurantId(restaurantId);

        // 刪除餐廳底下所有的抽選紀錄
        restaurantSelectionService.deleteAllRestaurantSelectionByRestaurantId(restaurantId);

        // 刪除餐廳
        restaurantRepository.delete(restaurant);
    }

    /** 取得餐廳資料 */
    public RestaurantEntity getRestaurant(Integer restaurantId) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到餐廳"));

        return restaurant;
    }

    /** 驗證餐廳分類是否屬於指定群組 */
    private void validateCategory(Integer groupId, Integer categoryId) {
        if (!restaurantRepository.existsByGroupIdAndCategoryId(groupId, categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "群組找不到此餐廳分類");
        }
    }
}
