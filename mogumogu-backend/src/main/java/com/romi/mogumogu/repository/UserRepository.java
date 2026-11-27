package com.romi.mogumogu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    /** 查詢群組所有成員 */
    List<UserEntity> findByGroupId(Integer groupId);

    /** 查詢群組所有成員依據群組排序 */
    Page<UserEntity> findByGroupIdOrderByDisplayOrderIdAsc(Integer groupId, Pageable pageable);

    /** 根據電子郵件查詢使用者資料 */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /** 檢查電子郵件是否已註冊 */
    boolean existsByEmailIgnoreCase(String email);

    /** 檢查指定角色是否存在 */
    boolean existsByRole(UserRole role);
}
