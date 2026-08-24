package com.romi.mogumogu.entity.history;

import java.util.Date;

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
@Table(comment = "餐廳選擇歷史表", name = "restaurant_selection_history")
public class RestaurantSelectionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "歷史紀錄 ID")
    private Integer historyId;

    @Column(comment = "所屬群組 ID", nullable = false)
    private Integer groupId;

    @ManyToOne(optional = false)
    @JoinColumn(comment = "餐廳 ID", name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurant;

    @Column(comment = "選擇時間", nullable = false)
    private Date selectedAt;
}