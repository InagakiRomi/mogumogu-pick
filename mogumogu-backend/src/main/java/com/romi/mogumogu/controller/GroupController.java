package com.romi.mogumogu.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.service.GroupService;
import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.group.GroupMemberResponse;
import com.romi.mogumogu.dto.group.GroupResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
@Tag(name = "groups", description = "群組")
public class GroupController {
    private final GroupService groupService;

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("")
    @Operation(summary = "取得所有群組(系統管理員)")
    public PageResponse<GroupResponse> getGroups(@ParameterObject Pageable pageable) {
        return groupService.getGroups(pageable);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "依群組ID取得群組資料")
    public GroupResponse getGroupById(@PathVariable Integer groupId) {
        return groupService.getGroupById(groupId);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "取得群組成員清單")
    public PageResponse<GroupMemberResponse> getGroupMembers(
            @PathVariable Integer groupId,
            @ParameterObject Pageable pageable) {
        return groupService.getGroupMembers(groupId, pageable);
    }

    @PostMapping("")
    @Operation(summary = "新增群組")
    public GroupResponse createGroup(String groupName) {
        return groupService.createGroup(groupName);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN')")
    @PostMapping("/{groupId}/members")
    @Operation(summary = "新增群組成員（管理員）")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse addGroupMember(
            @PathVariable Integer groupId,
            @RequestParam String email) {
        return groupService.addGroupMember(groupId, email);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN')")
    @PatchMapping(value = "/{groupId}", consumes = "text/plain")
    @Operation(summary = "修改群組（管理員）")
    public GroupResponse updateGroup(
            @PathVariable Integer groupId,
            @Size(max = 32) @RequestBody String groupName) {
        return groupService.updateGroup(groupId, groupName);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN')")
    @PostMapping("/{groupAdminUserId}/transfer-admin/{transferUserId}")
    @Operation(summary = "移轉群組管理權（管理員）")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferGroupAdmin(
            @PathVariable Integer groupAdminUserId,
            @PathVariable Integer transferUserId) {
        groupService.transferGroupAdmin(groupAdminUserId, transferUserId);
    }

    @PostMapping("/leave/{userId}")
    @Operation(summary = "退出群組")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(@PathVariable Integer userId) {
        groupService.leaveGroup(userId);
    }
}
