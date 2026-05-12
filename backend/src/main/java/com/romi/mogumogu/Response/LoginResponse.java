package com.romi.mogumogu.Response;

import java.util.Date;

import com.romi.mogumogu.enums.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    /** 使用者 ID */
    private Integer userId;

    /** 所屬群組 ID */
    private Integer groupId;

    /** 電子郵件 */
    private String email;

    /** 使用者名稱 */
    private String username;

    /** 角色 */
    private UserRole role;

    /** 帳號建立時間 */
    private Date createdAt;

    /** 帳號最後更新時間 */
    private Date updatedAt;
}
