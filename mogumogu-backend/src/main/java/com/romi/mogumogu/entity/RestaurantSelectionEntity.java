package com.romi.mogumogu.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(comment = "餐廳選擇歷史表", name = "restaurant_selection")
public class RestaurantSelectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "歷史紀錄ID")
    private Integer selectionId;

    @Column(comment = "所屬群組ID", nullable = false)
    private Integer groupId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", comment = "餐廳資料", nullable = false)
    private RestaurantEntity restaurant;

    @Column(comment = "選擇時間", nullable = false)
    @CreationTimestamp
    private LocalDateTime selectedAt;
}