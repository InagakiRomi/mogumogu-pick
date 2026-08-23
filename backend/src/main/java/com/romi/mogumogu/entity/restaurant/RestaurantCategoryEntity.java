package com.romi.mogumogu.entity.restaurant;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(comment = "餐廳分類表", name = "restaurant_category")
public class RestaurantCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "分類 ID")
    private Integer categoryId;

    @Column(comment = "所屬群組 ID", nullable = false)
    private Integer groupId;

    @Column(comment = "群組內排序 ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "分類名稱", length = 32, nullable = false)
    private String categoryName;

    @Column(comment = "資料建立時間")
    private Date createdAt;
}