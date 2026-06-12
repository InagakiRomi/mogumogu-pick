package com.romi.mogumogu.service.group;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.Response.GroupMemberResponse;
import com.romi.mogumogu.Response.GroupProfileResponse;
import com.romi.mogumogu.dto.AddGroupMemberDto;
import com.romi.mogumogu.dto.TransferGroupAdminDto;
import com.romi.mogumogu.dto.UpdateGroupNameDto;
import com.romi.mogumogu.entity.group.GroupEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.group.GroupRepository;
import com.romi.mogumogu.repository.user.UserRepository;
import com.romi.mogumogu.security.SecurityUtils;

@Service
public class GroupService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public GroupService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    /** 取得自己所屬群組成員清單 */
    public List<GroupMemberResponse> getMyGroupMembers() {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        Integer groupId = requireGroupId(currentUser);

        return userRepository.findByGroupIdOrderByDisplayOrderIdAscUserIdAsc(groupId).stream()
                .map(GroupMemberResponse::fromUser)
                .toList();
    }

    /** 新增群組成員（僅群組管理員） */
    @Transactional
    public GroupMemberResponse addGroupMember(AddGroupMemberDto request) {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        Integer groupId = requireGroupId(currentUser);
        ensureGroupAdmin(currentUser);

        // 取得目標使用者
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        UserEntity targetUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        // 檢查目標使用者是否為自己
        if (Objects.equals(targetUser.getUserId(), currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already in this group");
        }

        // 檢查目標使用者是否已經隸屬任何群組
        if (targetUser.getGroupId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target user already belongs to a group");
        }

        // 新增群組成員
        Date now = new Date();
        int maxDisplayOrder = Optional.ofNullable(userRepository.findMaxDisplayOrderIdByGroupId(groupId)).orElse(0);
        targetUser.setGroupId(groupId);
        targetUser.setDisplayOrderId(maxDisplayOrder + 1);
        targetUser.setRoles(UserRole.USER);
        targetUser.setUpdatedAt(now);

        UserEntity saved = userRepository.save(targetUser);
        return GroupMemberResponse.fromUser(saved);
    }

    /** 刪除群組成員（僅群組管理員） */
    @Transactional
    public void removeGroupMember(Integer targetUserId) {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        Integer groupId = requireGroupId(currentUser);
        ensureGroupAdmin(currentUser);

        // 檢查目標使用者是否為自己
        if (Objects.equals(currentUser.getUserId(), targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove yourself via this endpoint");
        }

        // 取得目標使用者
        UserEntity targetUser = userRepository.findById(Objects.requireNonNull(targetUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        // 檢查目標使用者是否為群組成員
        if (!Objects.equals(targetUser.getGroupId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Target user is not in your group");
        }

        // 檢查目標使用者是否為群組管理員
        if (targetUser.getRoles() == UserRole.GROUP_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove current group admin");
        }

        // 刪除群組成員
        Date now = new Date();
        targetUser.setGroupId(null);
        targetUser.setDisplayOrderId(0);
        targetUser.setRoles(UserRole.USER);
        targetUser.setUpdatedAt(now);
        userRepository.save(targetUser);
    }

    /** 移轉群組管理權（僅群組管理員） */
    @Transactional
    public GroupMemberResponse transferGroupAdmin(TransferGroupAdminDto request) {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        Integer groupId = requireGroupId(currentUser);
        ensureGroupAdmin(currentUser);

        // 取得目標使用者
        Integer targetUserId = Objects.requireNonNull(request.getTargetUserId());
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        // 檢查目標使用者是否為群組成員
        if (!Objects.equals(targetUser.getGroupId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Target user is not in your group");
        }
        // 檢查目標使用者是否為自己
        if (Objects.equals(targetUser.getUserId(), currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is already group admin");
        }

        // 移轉群組管理權
        Date now = new Date();
        currentUser.setRoles(UserRole.USER);
        currentUser.setUpdatedAt(now);
        targetUser.setRoles(UserRole.GROUP_ADMIN);
        targetUser.setUpdatedAt(now);
        userRepository.save(currentUser);
        UserEntity savedTarget = userRepository.save(targetUser);

        return GroupMemberResponse.fromUser(savedTarget);
    }

    /** 取得目前群組名稱 */
    public GroupProfileResponse getMyGroupProfile() {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();

        // 取得目前登入使用者所屬群組
        Integer groupId = requireGroupId(currentUser);
        GroupEntity group = findGroupOrThrow(groupId);

        return GroupProfileResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .build();
    }

    /** 修改目前群組名稱（僅群組管理員） */
    @Transactional
    public GroupProfileResponse updateMyGroupName(UpdateGroupNameDto request) {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        // 取得目前登入使用者所屬群組
        Integer groupId = requireGroupId(currentUser);
        // 檢查目前登入使用者是否為群組管理員
        ensureGroupAdmin(currentUser);

        // 取得目前群組
        GroupEntity group = findGroupOrThrow(groupId);
        group.setGroupName(request.getGroupName().trim());
        group.setUpdatedAt(new Date());
        GroupEntity saved = groupRepository.save(group);

        return GroupProfileResponse.builder()
                .groupId(saved.getGroupId())
                .groupName(saved.getGroupName())
                .build();
    }

    /** 自行退出群組 */
    @Transactional
    public void leaveMyGroup() {
        // 取得目前登入使用者
        UserEntity currentUser = getCurrentUserOrThrow();
        // 確認使用者已加入群組
        requireGroupId(currentUser);

        // 群組管理員須先移轉管理權才能退出
        if (currentUser.getRoles() == UserRole.GROUP_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please transfer group admin before leaving");
        }

        // 退出群組
        Date now = new Date();
        currentUser.setGroupId(null);
        currentUser.setDisplayOrderId(0);
        currentUser.setRoles(UserRole.USER);
        currentUser.setUpdatedAt(now);
        userRepository.save(currentUser);
    }

    /** 從 Security Context 取得目前登入使用者 */
    private UserEntity getCurrentUserOrThrow() {
        Integer userId = Objects.requireNonNull(SecurityUtils.getCurrentUserId());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** 確認使用者已加入群組，否則拋出 400 */
    private Integer requireGroupId(UserEntity user) {
        if (user.getGroupId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not in a group");
        }
        return user.getGroupId();
    }

    /** 確認使用者為群組管理員，否則拋出 403 */
    private void ensureGroupAdmin(UserEntity user) {
        if (user.getRoles() != UserRole.GROUP_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can perform this action");
        }
    }

    /** 依群組 ID 查詢群組實體，不存在則拋出 404 */
    private GroupEntity findGroupOrThrow(Integer groupId) {
        return groupRepository.findById(Objects.requireNonNull(groupId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }
}
