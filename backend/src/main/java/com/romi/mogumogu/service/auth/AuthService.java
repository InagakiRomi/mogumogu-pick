package com.romi.mogumogu.service.auth;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.Response.LoginResponse;
import com.romi.mogumogu.dto.LoginRequest;
import com.romi.mogumogu.entity.user.UserEntity;
import com.romi.mogumogu.repository.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        // 回傳登入成功資訊
        return LoginResponse.builder()
                .userId(user.getUserId())
                .groupId(user.getGroupId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRoles())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
