package com.romi.mogumogu.enums;

import java.util.Optional;

/** 使用者角色 */
public enum UserRole {
    /** 系統管理員 */
    SYSTEM_ADMIN,
    /** 群組管理員 */
    GROUP_ADMIN,
    /** 一般使用者 */
    USER;

    /** 從 name 回傳 UserRole */
    public static Optional<UserRole> fromName(String name) {
        // 檢查 name 是否為空
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        // 嘗試回傳 UserRole
        try {
            return Optional.of(valueOf(name));
        } catch (IllegalArgumentException ex) {
            // 如果 name 不合法，回傳 empty
            return Optional.empty();
        }
    }
}
