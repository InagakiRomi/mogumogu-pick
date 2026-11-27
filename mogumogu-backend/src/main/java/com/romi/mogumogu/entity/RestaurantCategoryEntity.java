package com.romi.mogumogu.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(comment = "餐廳分類表", name = "restaurant_category")
public class RestaurantCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "分類ID")
    private Integer categoryId;

    @Column(name = "group_id", comment = "所屬群組ID", nullable = false)
    private Integer groupId;

    @Column(comment = "群組內排序ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "分類名稱", nullable = false)
    private String categoryName;

    @Column(comment = "資料建立時間", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}