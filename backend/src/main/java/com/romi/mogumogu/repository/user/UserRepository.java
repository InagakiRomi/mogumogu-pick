package com.romi.mogumogu.repository.user;

import java.util.Optional;
import java.util.List;

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

    /** 根據群組 ID 查詢最大顯示順序 ID */
    @Query(value = "SELECT COALESCE(MAX(display_order_id), 0) FROM `user` WHERE group_id = :groupId", nativeQuery = true)
    Integer findMaxDisplayOrderIdByGroupId(@Param("groupId") Integer groupId);

    /** 查詢群組所有成員（依排序與使用者 ID） */
    List<UserEntity> findByGroupIdOrderByDisplayOrderIdAscUserIdAsc(Integer groupId);

    /** 檢查指定使用者是否屬於指定群組 */
    boolean existsByUserIdAndGroupId(Integer userId, Integer groupId);

    /** 查詢指定群組成員數 */
    long countByGroupId(Integer groupId);

    /** 查詢指定群組且指定角色的成員數 */
    long countByGroupIdAndRoles(Integer groupId, UserRole role);

    /** 查詢指定群組中的指定使用者 */
    Optional<UserEntity> findByUserIdAndGroupId(Integer userId, Integer groupId);
}
