package com.romi.mogumogu.entity.restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Comment("餐廳表")
public class Restaurant {
    @Comment("餐廳 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer restaurantId;

    @Comment("所屬群組 ID")
    @Column(nullable = false)
    private Integer groupId;

    @Comment("餐廳分類")
    @Column(length = 32, nullable = false)
    private RestaurantCategory categoryId;

    @Comment("群組內排序 ID")
    @Column(nullable = false)
    private Integer displayOrder;

    @Comment("餐廳名稱")
    @Column(length = 64, nullable = false)
    private String restaurantName;

    @Comment("餐廳圖片URL")
    @Column(length = 512)
    private String imageUrl;

    @Comment("被選中的累計次數")
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer selectedCount;

    @Comment("最後一次被選中的時間")
    private Date lastSelectedAt;

    @Comment("資料建立時間")
    private Date createdAt;

    @Comment("資料最後更新時間")
    private Date updatedAt;

    @Comment("補充說明或備註")
    @Column(length = 512)
    private String note;
}
