package com.romi.mogumogu.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(comment = "餐廳表", name = "restaurant")
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "餐廳ID")
    private Integer restaurantId;

    @Column(comment = "所屬群組ID", nullable = false)
    private Integer groupId;

    @Column(comment = "餐廳分類ID", nullable = false)
    private Integer categoryId;

    @Column(comment = "群組內排序ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "被選中的累計次數", nullable = false)
    private Integer selectedCount;

    @Column(comment = "餐廳名稱", nullable = false)
    private String restaurantName;

    @Column(comment = "餐廳地址")
    private String address;

    @Column(comment = "補充說明或備註")
    private String note;

    @Column(comment = "餐廳圖片URL")
    private String imageUrl;

    @Column(comment = "最後一次被選中的時間")
    private LocalDateTime lastSelectedAt;

    @Column(comment = "資料建立時間", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(comment = "資料最後更新時間")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}