package com.romi.mogumogu.entity.history;

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

import java.util.Date;

import org.hibernate.annotations.Comment;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Comment("餐廳選擇歷史表")
@Table(name = "restaurant_selection_history")
public class RestaurantSelectionHistoryEntity {

    @Comment("歷史紀錄 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historyId;

    @Comment("所屬群組 ID")
    @Column(nullable = false)
    private Integer groupId;

    @Comment("餐廳 ID")
    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantEntity restaurant;

    @Comment("選擇時間")
    @Column(nullable = false)
    private Date selectedAt;
}
