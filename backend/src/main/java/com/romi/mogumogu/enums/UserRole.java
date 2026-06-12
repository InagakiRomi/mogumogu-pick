package com.romi.mogumogu.enums;

import java.util.Optional;

/** 使用者角色 */
public enum UserRole {
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

    /** 從 DB 數字代碼回傳 UserRole */
    public static Optional<UserRole> fromCode(Integer code) {
        // 檢查 code 是否為空
        if (code == null) {
            return Optional.empty();
        }

        // 檢查 code 是否在範圍內
        UserRole[] values = values();
        if (code < 0 || code >= values.length) {
            return Optional.empty();
        }

        return Optional.of(values[code]);
    }
}
