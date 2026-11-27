package com.romi.mogumogu.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCategoryResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantCategoryUpdateRequest;
import com.romi.mogumogu.entity.RestaurantCategoryEntity;
import com.romi.mogumogu.helper.UserHelper;
import com.romi.mogumogu.repository.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantCategoryService {
    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "飯類",
            "麵類",
            "火鍋",
            "燒烤",
            "速食",
            "早午餐",
            "甜點",
            "飲料",
            "其他");

    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserHelper userHelper;

    /** 取得所有餐廳分類(系統管理員) */
    public PageResponse<RestaurantCategoryResponse> getCategories(Pageable pageable) {
        // 取得餐廳分類分頁資料
        Page<RestaurantCategoryEntity> category = restaurantCategoryRepository.findAll(pageable);

        // 將Entity轉換成Response DTO
        Page<RestaurantCategoryResponse> response = category.map(RestaurantCategoryResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 取得指定群組的分類清單 */
    public PageResponse<RestaurantCategoryResponse> getCategoriesByGroupId(Integer groupId, Pageable pageable) {
        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(groupId);

        // 取得餐廳分類分頁資料
        Page<RestaurantCategoryEntity> category = restaurantCategoryRepository.findByGroupId(groupId, pageable);

        // 將Entity轉換成Response DTO
        Page<RestaurantCategoryResponse> response = category.map(RestaurantCategoryResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 新增目前使用者群組的分類 */
    public RestaurantCategoryResponse createGroupCategory(String categoryName) {
        // 取得目前登入使用者群組id
        Integer groupId = userHelper.getUserGroupId();

        return createCategory(groupId, categoryName);
    }

    /** 修改分類 */
    public RestaurantCategoryResponse updateCategory(Integer categoryId,
            RestaurantCategoryUpdateRequest restaurantCategoryUpdateRequest) {
        // 取得資料
        RestaurantCategoryEntity category = getCategory(categoryId);
        Integer updateCategoryGroupId = category.getGroupId();

        // 檢查分類是否屬於目前使用者
        userHelper.validateGroupAccess(updateCategoryGroupId);

        // 修改分類名稱
        String categoryName = restaurantCategoryUpdateRequest.categoryName();

        if (categoryName != null && !categoryName.isBlank()) {
            // 檢查同群組內是否有重複分類名稱，且略過自己
            if (restaurantCategoryRepository.existsByGroupIdAndCategoryNameAndCategoryIdNot(
                    updateCategoryGroupId, categoryName, categoryId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "已有重複的分類名稱");
            }
            category.setCategoryName(categoryName);
        }

        // 修改顯示排序
        Integer displayOrderId = restaurantCategoryUpdateRequest.displayOrderId();

        if (displayOrderId != null) {
            if (restaurantCategoryRepository.existsByGroupIdAndDisplayOrderIdAndCategoryIdNot(
                    updateCategoryGroupId, displayOrderId, categoryId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "群組內已經有排序編號" + displayOrderId);
            }
            category.setDisplayOrderId(displayOrderId);
        }

        // 儲存新的分類資料
        RestaurantCategoryEntity saved = restaurantCategoryRepository.save(category);

        return RestaurantCategoryResponse.from(saved);
    }

    /** 刪除分類 */
    public void deleteCategory(Integer categoryId) {
        // 取得目前分類的資料
        RestaurantCategoryEntity category = getCategory(categoryId);
        Integer groupId = category.getGroupId();

        // 檢查分類是否屬於目前使用者
        userHelper.validateGroupAccess(category.getGroupId());

        // 檢查是否為最後一個分類
        if (restaurantCategoryRepository.countByGroupId(groupId) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能刪除僅有的分類");
        }

        // 檢查是否有餐廳使用此分類
        if (restaurantRepository.existsByGroupIdAndCategoryId(groupId, categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此分類仍有餐廳使用，無法刪除");
        }

        restaurantCategoryRepository.delete(category);
    }

    /** 建立預設分類 */
    public void addDefaultCategory(Integer groupId) {
        for (String string : DEFAULT_CATEGORY_NAMES) {
            createCategory(groupId, string);
        }
    }

    /** 新增分類 */
    private RestaurantCategoryResponse createCategory(Integer groupId, String categoryName) {
        // 檢查同群組內是否有重複分類名稱
        if (restaurantCategoryRepository.existsByGroupIdAndCategoryName(groupId, categoryName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已有重複的分類名稱");
        }

        // 計算群組內排序
        Integer displayOrderId = restaurantCategoryRepository.findMaxDisplayOrderIdByGroupId(groupId) + 1;

        // 建立新分類
        RestaurantCategoryEntity restaurantCategoryEntity = new RestaurantCategoryEntity();

        restaurantCategoryEntity.setGroupId(groupId);
        restaurantCategoryEntity.setDisplayOrderId(displayOrderId);
        restaurantCategoryEntity.setCategoryName(categoryName);

        // 儲存至資料庫
        RestaurantCategoryEntity savedCategory = restaurantCategoryRepository.save(restaurantCategoryEntity);

        return RestaurantCategoryResponse.from(savedCategory);
    }

    /** 取得餐廳分類 */
    private RestaurantCategoryEntity getCategory(Integer categoryId) {
        return restaurantCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "餐廳分類不存在"));
    }
}
