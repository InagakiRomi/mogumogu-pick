package com.romi.mogumogu.service.restaurant;

import com.romi.mogumogu.Response.RestaurantResponse;
import com.romi.mogumogu.dto.CreateRestaurantDto;
import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import com.romi.mogumogu.repository.restaurant.RestaurantCategoryRepository;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    public List<RestaurantResponse> getRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(RestaurantResponse::restaurantResponse)
                .toList();
    }

    /** 新增餐廳 */
    public RestaurantResponse createRestaurant(CreateRestaurantDto request) {
        // 取得群組 ID
        Integer groupId = request.getGroupId();
        if (!restaurantCategoryRepository.existsByGroupId(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "該群組不存在");
        }

        // 取得分類
        var categoryOptional = restaurantCategoryRepository
                .findByCategoryIdAndGroupId(request.getCategoryId(), groupId);
        if (categoryOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "該分類不存在");
        }

        // 取得同群組內目前最大的 displayOrder
        RestaurantEntity latestRestaurant = restaurantRepository.findTopByGroupIdOrderByDisplayOrderDesc(groupId);
        Integer nextDisplayOrder;
        if (latestRestaurant == null || latestRestaurant.getDisplayOrder() == null) {
            nextDisplayOrder = 1;
        } else {
            nextDisplayOrder = latestRestaurant.getDisplayOrder() + 1;
        }

        // 新增餐廳
        var category = categoryOptional.get();
        Date now = new Date();
        RestaurantEntity entity = Objects.requireNonNull(RestaurantEntity.builder()
                .groupId(groupId)
                .categoryId(category)
                .displayOrder(nextDisplayOrder)
                .restaurantName(request.getRestaurantName())
                .note(request.getNote())
                .imageUrl(request.getImageUrl())
                .selectedCount(0)
                .lastSelectedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build());

        RestaurantEntity savedEntity = restaurantRepository.save(entity);
        return RestaurantResponse.restaurantResponse(savedEntity);
    }
}
