package com.romi.mogumogu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantRandomResponse;
import com.romi.mogumogu.dto.restaurant.RestaurantResponse;
import com.romi.mogumogu.entity.RestaurantEntity;
import com.romi.mogumogu.helper.UserHelper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantRandomService {
    private final RestaurantService restaurantService;
    private final RestaurantSelectionService restaurantSelectionService;
    private final UserHelper userHelper;

    /** 抽取自己所屬群組的一間餐廳 */
    public RestaurantRandomResponse getRandomRestaurant(List<Integer> randomPool, Integer categoryId) {
        // 取得目前登入使用者群組ID
        Integer groupId = userHelper.getUserGroupId();

        // 總筆數
        long randomTotal = 0;

        // 還沒抽完就不重覆抓資料
        if (randomPool.size() <= 0) {
            // 取得自己所屬群組的餐廳清單
            PageResponse<RestaurantResponse> restaurantList = restaurantService.getRestaurants(
                    groupId, null, categoryId, Pageable.unpaged());

            // 獲取總筆數
            randomTotal = restaurantList.total();

            // 檢查餐廳列表是否為空
            if (randomTotal <= 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需要先建立餐廳才可以抽選");
            }

            // 儲存餐廳ID
            for (RestaurantResponse restaurant : restaurantList.data()) {
                randomPool.add(restaurant.restaurantId());
            }
        }

        // 隨機取得一筆餐廨ID
        int randomIndex = ThreadLocalRandom.current().nextInt(randomPool.size());
        Integer restaurantId = randomPool.get(randomIndex);

        // 抽籤池移除抽到的餐廳編號
        randomPool.remove(restaurantId);

        // 取得選中的餐廳
        RestaurantResponse selectedRestaurant = restaurantService.getRestaurantById(restaurantId);

        // 計算剩餘總數
        Integer remainingCount = randomPool.size();

        return RestaurantRandomResponse.from(selectedRestaurant, randomTotal, remainingCount);
    }

    /** 選擇餐廳 */
    @Transactional
    public RestaurantResponse chooseRestaurant(List<Integer> randomPool, Integer restaurantId) {
        // 取得餐廳資料
        RestaurantEntity restaurant = restaurantService.getRestaurant(restaurantId);

        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 更新選取紀錄
        restaurant.setSelectedCount(restaurant.getSelectedCount() + 1);
        restaurant.setLastSelectedAt(LocalDateTime.now());

        // 建立抽選紀錄
        restaurantSelectionService.createGroupSelection(restaurantId);

        // 重置抽籤池
        clearRandomPool(randomPool);

        return RestaurantResponse.from(restaurant);
    }

    /** 重置抽籤池 */
    public void clearRandomPool(List<Integer> randomPool) {
        randomPool.clear();
    }
}
