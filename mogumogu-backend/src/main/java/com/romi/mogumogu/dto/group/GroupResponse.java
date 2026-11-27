package com.romi.mogumogu.dto.group;

import java.time.LocalDateTime;

import com.romi.mogumogu.entity.GroupEntity;

public record GroupResponse(
        Integer groupId,
        String groupName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static GroupResponse from(GroupEntity group) {
        return new GroupResponse(
                group.getGroupId(),
                group.getGroupName(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
