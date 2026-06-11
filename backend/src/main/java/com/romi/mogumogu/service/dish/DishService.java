package com.romi.mogumogu.service.dish;

import com.romi.mogumogu.Response.DishListResponse;
import com.romi.mogumogu.Response.DishResponse;
import com.romi.mogumogu.dto.CreateDishDto;
import com.romi.mogumogu.dto.UpdateDishDto;
import com.romi.mogumogu.entity.dish.DishEntity;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.repository.dish.DishRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final RestaurantRepository restaurantRepository;

    public DishService(DishRepository dishRepository, RestaurantRepository restaurantRepository) {
        this.dishRepository = dishRepository;
        this.restaurantRepository = restaurantRepository;
    }

    /** 取得餐廳對應的全部餐點 */
    public DishListResponse getRestaurantDishes(Integer restaurantId) {
        // 檢查餐廳是否存在
        findRestaurantOrThrow(restaurantId);

        // 取得餐廳對應的全部餐點
        List<DishResponse> dishes = dishRepository
                .findByRestaurantId_RestaurantIdOrderByDisplayOrderIdAsc(restaurantId)
                .stream()
                .map(DishResponse::dishResponse)
                .toList();

        return DishListResponse.builder()
                .data(dishes)
                .total(dishes.size())
                .build();
    }

    /** 新增餐點 */
    @SuppressWarnings("null")
    public DishResponse createDish(CreateDishDto request) {
        // 取得餐廳 ID
        Integer restaurantId = request.getRestaurantId();
        // 檢查餐廳是否存在
        RestaurantEntity restaurant = findRestaurantOrThrow(restaurantId);

        // 取得餐廳內目前最大的 displayOrderId
        DishEntity latestDish = dishRepository
                .findTopByRestaurantId_RestaurantIdOrderByDisplayOrderIdDesc(restaurantId);

        // 計算下一個 displayOrderId
        Integer nextDisplayOrderId;

        // 如果餐廳內沒有餐點，則下一個 displayOrderId 為 1
        // 如果餐廳內有餐點，則下一個 displayOrderId 為目前最大的 displayOrderId + 1
        if (latestDish == null || latestDish.getDisplayOrderId() == null) {
            nextDisplayOrderId = 1;
        } else {
            nextDisplayOrderId = latestDish.getDisplayOrderId() + 1;
        }

        // 建立餐點實體
        DishEntity entity = DishEntity.builder()
                .restaurantId(restaurant)
                .displayOrderId(nextDisplayOrderId)
                .price(Objects.requireNonNull(request.getPrice()))
                .dishName(request.getDishName())
                .build();

        DishEntity savedEntity = Objects.requireNonNull(dishRepository.save(entity));
        return DishResponse.dishResponse(savedEntity);
    }

    /** 修改餐點排序、名稱與價格 */
    public DishResponse updateDish(Integer dishId, UpdateDishDto request) {
        // 檢查餐點是否存在
        DishEntity dish = findDishOrThrow(dishId);
        // 取得修改後的 displayOrderId
        Integer displayOrderId = Objects.requireNonNull(request.getDisplayOrderId());
        // 取得餐廳 ID
        Integer restaurantId = dish.getRestaurantId().getRestaurantId();

        // 檢查同一餐廳內是否已有其他餐點使用此 displayOrderId
        if (dishRepository.existsByRestaurantId_RestaurantIdAndDisplayOrderIdAndDishIdNot(
                restaurantId, displayOrderId, dish.getDishId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "displayOrderId already exists in this restaurant");
        }

        // 修改餐點排序、名稱與價格
        dish.setDisplayOrderId(displayOrderId);
        dish.setDishName(request.getDishName());
        dish.setPrice(Objects.requireNonNull(request.getPrice()));

        DishEntity savedEntity = Objects.requireNonNull(dishRepository.save(dish));
        return DishResponse.dishResponse(savedEntity);
    }

    /** 刪除餐點 */
    @SuppressWarnings("null")
    public void deleteDish(Integer dishId) {
        // 檢查餐點是否存在
        DishEntity dish = findDishOrThrow(dishId);

        // 刪除餐點實體
        dishRepository.delete(dish);
    }

    /** 檢查餐廳是否存在 */
    private RestaurantEntity findRestaurantOrThrow(Integer restaurantId) {
        if (restaurantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restaurantId must not be null");
        }
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }

    /** 檢查餐點是否存在 */
    private DishEntity findDishOrThrow(Integer dishId) {
        if (dishId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dishId must not be null");
        }
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
    }
}
