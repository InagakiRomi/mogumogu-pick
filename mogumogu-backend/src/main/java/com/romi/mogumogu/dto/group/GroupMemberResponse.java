package com.romi.mogumogu.dto.group;

import java.time.LocalDateTime;

import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;

public record GroupMemberResponse(
        Integer displayOrderId,
        UserRole role,
        String username,
        String email,
        LocalDateTime createdAt) {
    public static GroupMemberResponse from(UserEntity user) {
        return new GroupMemberResponse(
                user.getDisplayOrderId(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt());
    }
}
