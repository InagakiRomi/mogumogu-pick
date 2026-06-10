package com.romi.mogumogu.service.auth;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.Response.LoginResponse;
import com.romi.mogumogu.config.JwtTokenProvider;
import com.romi.mogumogu.dto.LoginRequest;
import com.romi.mogumogu.dto.RegisterRequest;
import com.romi.mogumogu.entity.group.GroupEntity;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.group.GroupRepository;
import com.romi.mogumogu.repository.user.UserRepository;

@Service
public class AuthService {

    private static final String DEFAULT_GROUP_NAME = "Group";

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            GroupRepository groupRepository,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 登入 */
    public LoginResponse login(LoginRequest request) {
        // 查詢使用者
        String email = request.getEmail().trim();
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User with email \"" + email + "\" not found");
        }
        UserEntity user = userOpt.get();

        // 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // 生成 JWT token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // 回傳登入成功資訊（含 JWT）
        return LoginResponse.builder()
                .userId(user.getUserId())
                .groupId(user.getGroupId())
                .role(user.getRoles().ordinal())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .token(accessToken)
                .build();
    }

    /** 註冊帳號 */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 檢查電子郵件是否已註冊
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
        }

        // 使用者名稱去掉空白字元
        String username = request.getUsername().trim();

        // 檢查角色是否有效
        UserRole requestedRole;
        if (request.getRole() == null) {
            requestedRole = UserRole.USER;
        } else {
            Optional<UserRole> roleOpt = UserRole.fromCode(request.getRole());
            if (roleOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
            requestedRole = roleOpt.get();
        }

        // 如果系統管理員存在，則不能註冊為系統管理員
        if (requestedRole == UserRole.SYSTEM_ADMIN && userRepository.existsByRole(UserRole.SYSTEM_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SYSTEM_ADMIN already exists");
        }

        // 建立新使用者
        Date now = new Date();
        UserEntity newUser = UserEntity.builder()
                .groupId(null)
                .displayOrderId(0)
                .roles(requestedRole)
                .username(username)
                .email(email)
                .userPassword(passwordEncoder.encode(request.getPassword()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserEntity saved = Objects.requireNonNull(
                userRepository.save(Objects.requireNonNull(newUser)));

        // 如果使用者是群組管理員，則建立群組
        if (requestedRole == UserRole.GROUP_ADMIN) {
            saved = createGroupForAdmin(saved, now);
        }

        return LoginResponse.builder()
                .userId(saved.getUserId())
                .groupId(saved.getGroupId())
                .role(saved.getRoles().ordinal())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    /** 建立群組 */
    private UserEntity createGroupForAdmin(UserEntity admin, Date now) {
        // 建立新群組
        int newGroupId = Optional.ofNullable(groupRepository.findMaxGroupId()).orElse(0) + 1;
        GroupEntity newGroup = GroupEntity.builder()
                .groupId(newGroupId)
                .groupName(DEFAULT_GROUP_NAME)
                .createdAt(now)
                .updatedAt(now)
                .build();
        GroupEntity savedGroup = Objects.requireNonNull(groupRepository.save(Objects.requireNonNull(newGroup)));

        // 設定使用者所屬群組
        admin.setGroupId(savedGroup.getGroupId());
        admin.setDisplayOrderId(1);
        admin.setUpdatedAt(now);

        // 儲存使用者
        return Objects.requireNonNull(userRepository.save(admin));
    }
}
