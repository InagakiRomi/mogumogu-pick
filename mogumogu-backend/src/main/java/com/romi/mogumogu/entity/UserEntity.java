package com.romi.mogumogu.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.romi.mogumogu.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(comment = "使用者表", name = "`user`")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "使用者ID")
    private Integer userId;

    @Column(comment = "所屬群組ID")
    private Integer groupId;

    @Column(comment = "群組內排序ID", nullable = false)
    private Integer displayOrderId;

    @Enumerated(EnumType.ORDINAL)
    @Column(comment = "使用者角色（0=群組管理員、1=一般使用者）", nullable = false)
    private UserRole role;

    @Column(comment = "使用者名稱", nullable = false)
    private String username;

    @Column(comment = "電子郵件", unique = true, nullable = false)
    private String email;

    @Column(comment = "密碼", nullable = false)
    private String passwordHash;

    @Column(comment = "帳號建立時間", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(comment = "帳號最後更新時間")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}