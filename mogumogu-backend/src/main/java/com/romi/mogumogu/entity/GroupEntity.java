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
@Table(comment = "群組表", name = "`group`")
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "群組ID")
    private Integer groupId;

    @Column(comment = "群組名稱", nullable = false)
    private String groupName;

    @Column(comment = "建立時間", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(comment = "更新時間")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}