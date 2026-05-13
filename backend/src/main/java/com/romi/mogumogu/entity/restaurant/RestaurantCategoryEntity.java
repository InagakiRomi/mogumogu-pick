package com.romi.mogumogu.entity.restaurant;

import java.util.Date;

import org.hibernate.annotations.Comment;

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
@Comment("餐廳分類表")
@Table(name = "restaurant_category")
public class RestaurantCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("分類 ID")
    private Integer categoryId;

    @Comment("所屬群組 ID")
    @Column(nullable = false)
    private Integer groupId;

    @Comment("群組內排序 ID")
    @Column(nullable = false)
    private Integer displayOrderId;

    @Comment("分類名稱")
    @Column(length = 32, nullable = false)
    private String categoryName;

    @Comment("資料建立時間")
    private Date createdAt;
}
