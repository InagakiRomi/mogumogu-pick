package com.romi.mogumogu.helper;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.GroupRepository;
import com.romi.mogumogu.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserHelper {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    /** 取得目前登入使用者ID */
    public Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "使用者尚未登入");
        }

        return Integer.valueOf(auth.getName());
    }

    /** 根據ID取得使用者 */
    public UserEntity getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "使用者不存在"));
    }

    /** 取得目前登入使用者 */
    public UserEntity getCurrentUser() {
        return getUserById(getCurrentUserId());
    }

    /** 取得目前登入使用者群組id */
    public Integer getUserGroupId() {
        UserEntity currentUser = getCurrentUser();

        if (currentUser.getGroupId() == null && currentUser.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此使用者尚未加入群組");
        }

        return currentUser.getGroupId();
    }

    /** 驗證目前使用者是否有指定群組的操作權限 */
    public void validateGroupAccess(Integer groupId) {
        UserEntity currentUser = getCurrentUser();

        // 確認群組是否存在
        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到群組");
        }

        if (currentUser.getRole() != UserRole.SYSTEM_ADMIN
                && !Objects.equals(currentUser.getGroupId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此使用者不屬於這個群組");
        }
    }
}
