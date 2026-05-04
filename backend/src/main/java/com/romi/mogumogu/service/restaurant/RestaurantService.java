package com.romi.mogumogu.service.restaurant;

import com.romi.mogumogu.dto.RestaurantDto;
import com.romi.mogumogu.repository.restaurant.RestaurantRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * 取得所有餐廳
     *
     * @return 餐廳列表
     */
    public List<RestaurantDto> getRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(RestaurantDto::restaurantResponse)
                .toList();
    }
}
