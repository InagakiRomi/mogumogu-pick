package com.romi.mogumogu.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.dish.DishCreateRequest;
import com.romi.mogumogu.dto.dish.DishResponse;
import com.romi.mogumogu.dto.dish.DishUpdateRequest;
import com.romi.mogumogu.entity.DishEntity;
import com.romi.mogumogu.entity.RestaurantEntity;
import com.romi.mogumogu.helper.UserHelper;
import com.romi.mogumogu.repository.DishRepository;
import com.romi.mogumogu.repository.RestaurantRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DishService {
    private final DishRepository dishRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserHelper userHelper;

    /** 取得指定餐廳的餐點清單 */
    public PageResponse<DishResponse> getDishesByRestaurantId(Integer restaurantId, Pageable pageable) {
        // 取得餐廳資料
        RestaurantEntity restaurant = getRestaurant(restaurantId);

        // 檢查餐廳是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 取得餐點分頁資料
        Page<DishEntity> dish = dishRepository.findByRestaurantIdOrderByDisplayOrderIdAsc(restaurantId, pageable);

        // 將Entity轉換成Response DTO
        Page<DishResponse> response = dish.map(DishResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 新增餐點 */
    public DishResponse createDish(DishCreateRequest dishCreateRequest) {
        // 取得餐廳資料
        Integer restaurantId = dishCreateRequest.restaurantId();
        RestaurantEntity restaurant = getRestaurant(restaurantId);

        // 檢查餐廳是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 檢查同餐廳內是否有重複餐點名稱
        if (dishRepository.existsByRestaurantIdAndDishName(restaurantId, dishCreateRequest.dishName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已有重複的餐點名稱");
        }

        // 計算餐廳內排序
        Integer displayOrderId = dishRepository.findMaxDisplayOrderIdByRestaurantId(restaurantId) + 1;

        // 建立新餐點
        DishEntity dish = new DishEntity();

        dish.setRestaurantId(restaurantId);
        dish.setDisplayOrderId(displayOrderId);
        dish.setPrice(dishCreateRequest.price());
        dish.setDishName(dishCreateRequest.dishName());

        // 儲存至資料庫
        DishEntity savedDish = dishRepository.save(dish);

        return DishResponse.from(savedDish);
    }

    /** 修改餐點 */
    public DishResponse updateDish(Integer dishId, DishUpdateRequest dishUpdateRequest) {
        // 取得餐點資料
        DishEntity dish = getDish(dishId);
        Integer restaurantId = dish.getRestaurantId();

        // 取得餐廳資料
        RestaurantEntity restaurant = getRestaurant(restaurantId);

        // 檢查餐廳是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 修改餐點名稱
        String dishName = dishUpdateRequest.dishName();

        if (dishName != null && !dishName.isBlank()) {
            // 檢查同餐廳內是否有重複餐點名稱，且略過自己
            if (dishRepository.existsByRestaurantIdAndDishNameAndDishIdNot(restaurantId, dishName, dishId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "已有重複的餐點名稱");
            }

            dish.setDishName(dishName);
        }

        // 修改餐點價格
        Integer price = dishUpdateRequest.price();

        if (price != null) {
            dish.setPrice(price);
        }

        // 儲存新的餐點資料
        DishEntity saved = dishRepository.save(dish);

        return DishResponse.from(saved);
    }

    /** 刪除餐點 */
    public void deleteDish(Integer dishId) {
        // 取得餐點資料
        DishEntity dish = getDish(dishId);

        // 取得餐廳資料
        RestaurantEntity restaurant = getRestaurant(dish.getRestaurantId());

        // 檢查餐廳是否屬於目前使用者的群組
        userHelper.validateGroupAccess(restaurant.getGroupId());

        // 刪除餐點
        dishRepository.delete(dish);
    }

    /** 刪除某餐廳底下所有餐點 */
    @Transactional
    public void deleteAllDishesByRestaurantId(Integer restaurantId) {
        dishRepository.deleteAllByRestaurantId(restaurantId);
    }

    /** 取得餐點 */
    private DishEntity getDish(Integer dishId) {
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "餐點不存在"));
    }

    /** 取得餐廳 */
    private RestaurantEntity getRestaurant(Integer restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "餐廳不存在"));
    }
}
