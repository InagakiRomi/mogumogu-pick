package com.romi.mogumogu.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantSelectionResponse;
import com.romi.mogumogu.entity.RestaurantEntity;
import com.romi.mogumogu.entity.RestaurantSelectionEntity;
import com.romi.mogumogu.helper.UserHelper;
import com.romi.mogumogu.repository.RestaurantRepository;
import com.romi.mogumogu.repository.RestaurantSelectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantSelectionService {
    private final RestaurantSelectionRepository restaurantSelectionRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserHelper userHelper;

    /** 取得指定群組的餐廳選擇歷史 */
    public PageResponse<RestaurantSelectionResponse> getSelectionsByGroupId(Integer groupId, Pageable pageable) {
        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(groupId);

        Page<RestaurantSelectionEntity> selections = restaurantSelectionRepository
                .findByGroupIdOrderBySelectedAtDesc(groupId, pageable);

        // 將Entity轉換成Response DTO
        Page<RestaurantSelectionResponse> response = selections.map(RestaurantSelectionResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 新增目前使用者群組的餐廳選擇紀錄 */
    @Transactional
    public RestaurantSelectionResponse createGroupSelection(Integer restaurantId) {
        // 取得目前登入使用者群組ID
        Integer groupId = userHelper.getUserGroupId();

        // 取得餐廳資料
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到餐廳"));

        // 檢查是否屬於目前使用者的群組
        if (!Objects.equals(groupId, restaurant.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能選擇別群組的餐廳");
        }

        // 建立選擇歷史
        RestaurantSelectionEntity selection = new RestaurantSelectionEntity();

        selection.setGroupId(groupId);
        selection.setRestaurant(restaurant);

        // 儲存選擇歷史
        RestaurantSelectionEntity saved = restaurantSelectionRepository.save(selection);

        return RestaurantSelectionResponse.from(saved);
    }

    /** 刪除餐廳選擇歷史 */
    @Transactional
    public void deleteSelection(Integer selectionId) {
        // 取得歷史紀錄
        RestaurantSelectionEntity selection = restaurantSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到選擇紀錄"));

        // 檢查群組存取權限
        userHelper.validateGroupAccess(selection.getGroupId());

        // 刪除歷史紀錄
        restaurantSelectionRepository.delete(selection);
    }

    /** 刪除某餐廳底下所有選擇紀錄 */
    @Transactional
    public void deleteAllRestaurantSelectionByRestaurantId(Integer restaurantId) {
        restaurantSelectionRepository.deleteAllByRestaurantId(restaurantId);
    }
}