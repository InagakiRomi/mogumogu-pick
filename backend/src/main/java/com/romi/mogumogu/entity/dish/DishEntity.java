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
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Comment("餐點表")
@Table(name = "dish")
public class DishEntity {

    @Comment("餐點 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dishId;

    @Comment("餐點所屬餐廳 ID")
    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurantId;

    @Comment("餐廳群組內順序 ID")
    @Column(nullable = false)
    private Integer displayOrderId;

    @Comment("餐點價格")
    @Column(nullable = false)
    private Integer price;

    @Comment("餐點名稱")
    @Column(length = 64, nullable = false)
    private String dishName;
}
