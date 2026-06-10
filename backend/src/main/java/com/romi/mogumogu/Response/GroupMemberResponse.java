package com.romi.mogumogu.Response;

import java.util.Date;

import com.romi.mogumogu.entity.user.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    /** 使用者 ID */
    private Integer userId;

    /** 群組 ID */
    private Integer groupId;

    /** 群組內排序 ID */
    private Integer displayOrderId;

    /** 角色代碼 */
    private Integer role;

    /** 使用者名稱 */
    private String username;

    /** 電子郵件 */
    private String email;

    /** 建立時間 */
    private Date createdAt;

    /** 更新時間 */
    private Date updatedAt;

    public static GroupMemberResponse fromUser(UserEntity user) {
        return GroupMemberResponse.builder()
                .userId(user.getUserId())
                .groupId(user.getGroupId())
                .displayOrderId(user.getDisplayOrderId())
                .role(user.getRoles().ordinal())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
