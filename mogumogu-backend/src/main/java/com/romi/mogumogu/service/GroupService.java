package com.romi.mogumogu.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.PageResponse;
import com.romi.mogumogu.dto.group.GroupMemberResponse;
import com.romi.mogumogu.dto.group.GroupResponse;
import com.romi.mogumogu.entity.GroupEntity;
import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.helper.UserHelper;
import com.romi.mogumogu.repository.GroupRepository;
import com.romi.mogumogu.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final RestaurantCategoryService restaurantCategoryService;
    private final UserHelper userHelper;

    /** 取得所有群組(系統管理員) */
    public PageResponse<GroupResponse> getGroups(Pageable pageable) {
        // 取得群組分頁資料
        Page<GroupEntity> group = groupRepository.findAll(pageable);

        // 將Entity轉換成Response DTO
        Page<GroupResponse> response = group.map(GroupResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 依群組ID取得群組資料 */
    public GroupResponse getGroupById(Integer groupId) {
        // 檢查是否屬於目前使用者的群組
        userHelper.validateGroupAccess(groupId);

        // 尋找群組
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到群組"));

        // 回傳群組資料
        return GroupResponse.from(group);
    }

    /** 取得取得群組成員清單 */
    public PageResponse<GroupMemberResponse> getGroupMembers(Integer groupId, Pageable pageable) {
        // 驗證群組管理權限
        userHelper.validateGroupAccess(groupId);

        // 取得群組成員清單
        Page<UserEntity> groupUsers = userRepository.findByGroupIdOrderByDisplayOrderIdAsc(groupId, pageable);

        // 將Entity轉換成Response DTO
        Page<GroupMemberResponse> response = groupUsers.map(GroupMemberResponse::from);

        // 包裝成自訂分頁格式
        return PageResponse.from(response);
    }

    /** 新增群組 */
    public GroupResponse createGroup(String groupName) {
        // 建立新群組
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setGroupName(groupName);

        // 儲存至資料庫
        GroupEntity savedGroup = groupRepository.save(groupEntity);

        // 建立預設餐廳分類
        restaurantCategoryService.addDefaultCategory(savedGroup.getGroupId());

        return GroupResponse.from(savedGroup);
    }

    /** 新增群組成員（管理員） */
    public GroupMemberResponse addGroupMember(Integer groupId, String email) {
        // 驗證群組管理權限
        userHelper.validateGroupAccess(groupId);

        // 取得目標使用者，未註冊報錯
        UserEntity addUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到使用者"));

        // 檢查目標使用者是否已屬於任何群組
        if (addUser.getGroupId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此使用者已經擁有群組");
        }

        // 檢查目標使用者是否是一般使用者
        if (addUser.getRole() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能新增一般使用者");
        }

        // 計算目前群組順位最大數
        int maxDisplayOrder = userRepository.findByGroupId(groupId)
                .stream()
                .mapToInt(UserEntity::getDisplayOrderId)
                .max()
                .orElse(0);

        // 新增群組成員
        addUser.setGroupId(groupId);
        addUser.setDisplayOrderId(maxDisplayOrder + 1);
        addUser.setRole(UserRole.USER);

        UserEntity saved = userRepository.save(addUser);

        return GroupMemberResponse.from(saved);
    }

    /** 修改群組（管理員） */
    public GroupResponse updateGroup(Integer groupId, String groupName) {
        // 驗證群組管理權限
        userHelper.validateGroupAccess(groupId);

        // 取得群組
        GroupEntity groupEntity = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "找不到群組"));

        // 儲存新的群組名稱
        groupEntity.setGroupName(groupName.trim());
        GroupEntity saved = groupRepository.save(groupEntity);

        return GroupResponse.from(saved);
    }

    /** 移轉群組管理權（管理員） */
    @Transactional
    public void transferGroupAdmin(Integer groupAdminUserId, Integer transferUserId) {
        // 取得群組管理員帳號
        UserEntity groupAdminUser = userHelper.getUserById(groupAdminUserId);

        // 取得目標使用者
        UserEntity targetUser = userHelper.getUserById(transferUserId);

        Integer groupId = groupAdminUser.getGroupId();

        // 驗證目標使用者身分
        if (groupAdminUser.getRole() != UserRole.SYSTEM_ADMIN
                || targetUser.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能移轉給系統管理員");
        }

        // 驗證原管理員是否已加入群組
        if (groupId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "使用者尚未加入群組");
        }

        // 驗證群組管理權限
        userHelper.validateGroupAccess(groupId);

        // 驗證不可移轉給相同帳號
        if (Objects.equals(groupAdminUserId, transferUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不可移轉給相同帳號");
        }

        // 驗證是否屬於相同群組
        if (!Objects.equals(groupId, targetUser.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能移轉給同群組的使用者");
        }

        if (targetUser.getRole() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能移轉給一般群組成員");
        }

        // 移轉群組管理權
        groupAdminUser.setRole(UserRole.USER);
        targetUser.setRole(UserRole.GROUP_ADMIN);
    }

    /** 退出群組 */
    public void leaveGroup(Integer userId) {
        // 取得目前登入帳號
        UserEntity currentUser = userHelper.getCurrentUser();

        // 取得目標使用者
        UserEntity targetUser = userHelper.getUserById(userId);
        Integer targetUserGroupId = targetUser.getGroupId();

        boolean isSelf = Objects.equals(currentUser.getUserId(), userId);
        boolean isSameGroup = Objects.equals(currentUser.getGroupId(), targetUserGroupId);
        UserRole currentRole = currentUser.getRole();

        // 確認使用者已加入群組
        if (targetUserGroupId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "使用者尚未加入群組");
        }

        // 一般使用者不得移除別人
        if (!isSelf && currentRole == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以移除別人");
        }

        // 群組管理員不得跨群組移除成員
        if (!isSelf && currentRole == UserRole.GROUP_ADMIN && !isSameGroup) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能移除不同群組的使用者");
        }

        // 群組管理員須先移轉管理權
        if (targetUser.getRole() == UserRole.GROUP_ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "群組管理員須先移轉管理權才能退出群組");
        }

        // 退出群組
        targetUser.setGroupId(null);
        targetUser.setDisplayOrderId(0);
        targetUser.setRole(UserRole.USER);
        userRepository.save(targetUser);
    }
}
