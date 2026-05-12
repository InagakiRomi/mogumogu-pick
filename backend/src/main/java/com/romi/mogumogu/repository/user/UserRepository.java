package com.romi.mogumogu.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.romi.mogumogu.entity.user.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    /** 根據電子郵件查詢使用者 */
    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
