package com.romi.mogumogu.entity.group;

import java.util.Date;

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
@Table(comment = "群組表", name = "user_group")
public class GroupEntity {

    @Id
    @Column(comment = "群組 ID")
    private Integer groupId;

    @Column(comment = "群組名稱", length = 64, nullable = false)
    private String groupName;

    @Column(comment = "建立時間")
    private Date createdAt;

    @Column(comment = "更新時間")
    private Date updatedAt;
}