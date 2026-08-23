package com.romi.mogumogu.entity.restaurant;

import java.util.Date;

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
@Table(comment = "餐廳表", name = "restaurant")
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "餐廳 ID")
    private Integer restaurantId;

    @Column(comment = "所屬群組 ID", nullable = false)
    private Integer groupId;

    @ManyToOne(optional = false)
    @JoinColumn(comment = "餐廳分類 ID", name = "category_id", nullable = false)
    private RestaurantCategoryEntity categoryId;

    @Column(comment = "群組內排序 ID", nullable = false)
    private Integer displayOrderId;

    @Column(comment = "被選中的累計次數", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer selectedCount;

    @Column(comment = "餐廳名稱", length = 64, nullable = false)
    private String restaurantName;

    @Column(comment = "補充說明或備註", length = 512)
    private String note;

    @Column(comment = "餐廳圖片URL", length = 512)
    private String imageUrl;

    @Column(comment = "最後一次被選中的時間")
    private Date lastSelectedAt;

    @Column(comment = "資料建立時間")
    private Date createdAt;

    @Column(comment = "資料最後更新時間")
    private Date updatedAt;
}