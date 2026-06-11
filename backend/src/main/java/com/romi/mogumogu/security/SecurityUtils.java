package com.romi.mogumogu.security;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.enums.UserRole;

/** 從 Spring Security 取得目前登入使用者資訊 */
public final class SecurityUtils {
    /** 取得目前登入使用者的 ID（JWT subject） */
    @NonNull
    public static Integer getCurrentUserId() {
        // 取得目前登入使用者的 Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw unauthorized();
        }

        // 取得目前登入使用者的 principal
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw unauthorized();
        }

        // 將 principal 轉換為 Integer
        return Integer.parseInt((String) principal);
    }

    /** 驗證目前登入使用者是否為 SYSTEM_ADMIN */
    public static void requireSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> ("ROLE_" + UserRole.SYSTEM_ADMIN.name()).equals(authority.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
