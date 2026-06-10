package com.romi.mogumogu.entity.group;

import java.util.Date;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Comment("群組表")
@Table(name = "user_group")
public class GroupEntity {
    @Id
    @Comment("群組 ID")
    private Integer groupId;

    @Comment("群組名稱")
    @Column(length = 64, nullable = false)
    private String groupName;

    @Comment("建立時間")
    private Date createdAt;

    @Comment("更新時間")
    private Date updatedAt;
}
