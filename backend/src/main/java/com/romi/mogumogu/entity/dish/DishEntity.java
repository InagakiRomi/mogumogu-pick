package com.romi.mogumogu.entity.dish;

import com.romi.mogumogu.entity.restaurant.RestaurantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(comment = "餐點表", name = "dish")
public class DishEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "餐點 ID")
    private Integer dishId;

    @ManyToOne(optional = false)
    @JoinColumn(comment = "餐點所屬餐廳 ID", name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurantId;

    @Column(comment = "餐廳群組內順序 ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "餐點價格", nullable = false)
    private Integer price;

    @Column(comment = "餐點名稱", length = 64, nullable = false)
    private String dishName;
}