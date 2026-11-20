package com.romi.mogumogu.entity.user;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.romi.mogumogu.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(comment = "使用者表", name = "`user`")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "使用者 ID")
    private Integer userId;

    @Column(comment = "所屬群組 ID")
    private Integer groupId;

    @Column(comment = "群組內排序 ID", nullable = false)
    private Integer displayOrderId;

    @Enumerated(EnumType.ORDINAL)
    @Column(comment = "使用者角色（0=群組管理員、1=一般使用者）", nullable = false)
    private UserRole roles;

    @Column(comment = "使用者名稱", length = 64, nullable = false)
    private String username;

    @Column(comment = "電子郵件", unique = true, length = 255, nullable = false)
    private String email;

    @JsonIgnore
    @Column(comment = "使用者密碼", name = "user_password", length = 255, nullable = false)
    private String userPassword;

    @Column(comment = "帳號建立時間")
    private Date createdAt;

    @Column(comment = "帳號最後更新時間")
    private Date updatedAt;
}