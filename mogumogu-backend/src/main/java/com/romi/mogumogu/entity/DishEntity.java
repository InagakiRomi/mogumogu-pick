package com.romi.mogumogu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(comment = "餐點表", name = "dish")
public class DishEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "餐點 ID")
    private Integer dishId;

    @Column(comment = "餐點所屬餐廳ID", nullable = false)
    private Integer restaurantId;

    @Column(comment = "餐廳群組內順序ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "餐點價格", nullable = false)
    private Integer price;

    @Column(comment = "餐點名稱", nullable = false)
    private String dishName;
}