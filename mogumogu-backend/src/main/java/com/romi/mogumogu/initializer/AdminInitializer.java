package com.romi.mogumogu.initializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String email;

    @Value("${admin.username}")
    private String username;

    @Value("${admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        // 未設定管理員帳密則跳過
        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            return;
        }

        // 已存在系統管理員則跳過
        if (userRepository.existsByRole(UserRole.SYSTEM_ADMIN)) {
            return;
        }

        // 避免與既有使用者的Email重複
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("管理員Email已被使用");
        }

        // 建立系統管理員
        UserEntity admin = new UserEntity();
        admin.setGroupId(null);
        admin.setDisplayOrderId(0);
        admin.setRole(UserRole.SYSTEM_ADMIN);
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));

        // 儲存至資料庫
        userRepository.save(admin);
    }
}