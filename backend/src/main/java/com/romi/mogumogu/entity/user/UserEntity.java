package com.romi.mogumogu.entity.user;

import java.util.Date;

import org.hibernate.annotations.Comment;

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
@Comment("使用者表")
@Table(name = "`user`")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("使用者 ID")
    private Integer userId;

    @Comment("所屬群組 ID")
    private Integer groupId;

    @Comment("群組內排序 ID")
    @Column(nullable = false)
    private Integer displayOrderId;

    @Comment("使用者名稱")
    @Column(length = 64, nullable = false)
    private String username;

    @Comment("電子郵件")
    @Column(unique = true, length = 255, nullable = false)
    private String email;

    @JsonIgnore
    @Comment("使用者密碼")
    @Column(name = "user_password", length = 255, nullable = false)
    private String userPassword;

    @Enumerated(EnumType.ORDINAL)
    @Comment("使用者角色（0=系統管理員、1=群組管理員、2=一般使用者）")
    @Column(nullable = false)
    private UserRole roles;

    @Comment("帳號建立時間")
    private Date createdAt;

    @Comment("帳號最後更新時間")
    private Date updatedAt;
}
