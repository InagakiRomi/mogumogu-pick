package com.romi.mogumogu.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    /** 根據電子郵件查詢使用者 */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /** 根據電子郵件查詢使用者是否存在 */
    boolean existsByEmailIgnoreCase(String email);

    /** 是否已有指定角色的使用者 */
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.roles = :role")
    boolean existsByRole(@Param("role") UserRole role);

    /** 根據群組 ID 查詢最大顯示順序 ID */
    @Query(value = "SELECT COALESCE(MAX(display_order_id), 0) FROM `user` WHERE group_id = :groupId", nativeQuery = true)
    Integer findMaxDisplayOrderIdByGroupId(@Param("groupId") Integer groupId);
}
