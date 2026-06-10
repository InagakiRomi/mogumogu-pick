package com.romi.mogumogu.controller.group;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.romi.mogumogu.Response.GroupMemberResponse;
import com.romi.mogumogu.Response.GroupProfileResponse;
import com.romi.mogumogu.dto.AddGroupMemberDto;
import com.romi.mogumogu.dto.TransferGroupAdminDto;
import com.romi.mogumogu.dto.UpdateGroupNameDto;
import com.romi.mogumogu.service.group.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/groups/my")
@Tag(name = "groups", description = "群組")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/members")
    @Operation(summary = "取得自己所屬群組成員清單")
    public List<GroupMemberResponse> getMyGroupMembers() {
        return groupService.getMyGroupMembers();
    }

    @PostMapping("/members")
    @Operation(summary = "新增群組成員")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse addGroupMember(@Valid @RequestBody AddGroupMemberDto request) {
        return groupService.addGroupMember(request);
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "刪除群組成員")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGroupMember(@PathVariable Integer userId) {
        groupService.removeGroupMember(userId);
    }

    @PostMapping("/transfer-admin")
    @Operation(summary = "移轉群組管理權")
    public GroupMemberResponse transferGroupAdmin(@Valid @RequestBody TransferGroupAdminDto request) {
        return groupService.transferGroupAdmin(request);
    }

    @GetMapping("")
    @Operation(summary = "取得目前群組名稱")
    public GroupProfileResponse getMyGroupProfile() {
        return groupService.getMyGroupProfile();
    }

    @PatchMapping("")
    @Operation(summary = "修改目前群組名稱")
    public GroupProfileResponse updateMyGroupName(@Valid @RequestBody UpdateGroupNameDto request) {
        return groupService.updateMyGroupName(request);
    }

    @PostMapping("/leave")
    @Operation(summary = "自行退出群組")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveMyGroup() {
        groupService.leaveMyGroup();
    }
}
