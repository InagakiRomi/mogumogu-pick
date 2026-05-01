package com.romi.mogumogu.service.restaurant;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
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
    public List<RestaurantEntity> getRestaurants() {
        return restaurantRepository.findAll();
    }
}
