package com.romi.mogumogu.dto.auth;

import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        Integer userId,
        Integer groupId,
        Integer displayOrderId,
        UserRole role,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static AuthResponse from(UserEntity user, String token) {
        return new AuthResponse(
                token,
                user.getUserId(),
                user.getGroupId(),
                user.getDisplayOrderId(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
