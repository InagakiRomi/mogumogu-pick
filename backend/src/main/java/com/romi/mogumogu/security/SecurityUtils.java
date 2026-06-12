package com.romi.mogumogu.security;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;


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

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
