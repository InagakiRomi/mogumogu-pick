package com.romi.mogumogu.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.auth.AuthResponse;
import com.romi.mogumogu.dto.auth.LoginRequest;
import com.romi.mogumogu.dto.auth.RegisterRequest;
import com.romi.mogumogu.dto.group.GroupResponse;
import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;

    /** 註冊帳號 */
    public AuthResponse register(RegisterRequest registerRequest) {
        // 建立資料
        UserRole role = registerRequest.role();
        String username = registerRequest.username().trim();
        String email = registerRequest.email().trim();
        String password = registerRequest.password();

        // 密碼加鹽
        String passwordHash = passwordEncoder.encode(password);

        // 檢查電子郵件是否已註冊
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email已經被註冊");
        }

        Integer newGroupId = null;
        Integer newDisplayOrderId = 0;

        // 如果使用者是群組管理員，則自動建立群組
        if (role == UserRole.GROUP_ADMIN) {
            // 新增群組
            GroupResponse groupResponse = groupService.createGroup(username + "的群組");
            newGroupId = groupResponse.groupId();
            newDisplayOrderId = 1;
        }

        // 建立新使用者
        UserEntity userEntity = new UserEntity();
        userEntity.setGroupId(newGroupId);
        userEntity.setDisplayOrderId(newDisplayOrderId);
        userEntity.setRole(role);
        userEntity.setUsername(username);
        userEntity.setEmail(email);
        userEntity.setPasswordHash(passwordHash);

        // 儲存至資料庫
        UserEntity savedUser = userRepository.save(userEntity);

        return AuthResponse.from(savedUser, null);
    }

    /** 登入 */
    public AuthResponse login(LoginRequest loginRequest) {
        // 建立資料
        String email = loginRequest.email().trim();
        String password = loginRequest.password();

        // 檢查Email是否已註冊，未註冊報錯
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤"));

        // 驗證密碼
        String passwordHash = user.getPasswordHash();
        if (!passwordEncoder.matches(password, passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
        }

        // 建立已驗證的使用者身分
        String userMail = user.getEmail();
        String userRole = "ROLE_" + user.getRole().name();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userMail,
                null,
                List.of(new SimpleGrantedAuthority(userRole)));

        // 生成JWTtoken
        String token = tokenService.issue(authentication, user.getUserId());

        return AuthResponse.from(user, token);
    }
}
