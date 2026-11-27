package com.romi.mogumogu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.romi.mogumogu.dto.auth.AuthResponse;
import com.romi.mogumogu.dto.auth.LoginRequest;
import com.romi.mogumogu.dto.auth.RegisterRequest;
import com.romi.mogumogu.dto.group.GroupResponse;
import com.romi.mogumogu.entity.UserEntity;
import com.romi.mogumogu.enums.UserRole;
import com.romi.mogumogu.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("會員註冊登入服務測試")
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private AuthService authService;

    // 共用測試資料
    private final UserRole role = UserRole.USER;
    private final String username = "TestAuto";
    private final String email = "testAuto@gmail.com";
    private final String password = "tESt!!24";
    private final String passwordHash = "hashedPassword";
    private final RegisterRequest registerRequest = new RegisterRequest(role, username, email, password);
    private final LoginRequest loginRequest = new LoginRequest(email, password);
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUserId(1);
        user.setRole(role);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
    }

    @ParameterizedTest
    @EnumSource
    @DisplayName("註冊成功")
    void register_success(UserRole role) {

        RegisterRequest registerRequest = new RegisterRequest(
                role, username, email, password);

        // 模擬Email尚未被註冊
        when(userRepository.existsByEmailIgnoreCase(email))
                .thenReturn(false);

        // 模擬密碼雜湊結果
        when(passwordEncoder.encode(password))
                .thenReturn(passwordHash);

        // 模擬群組管理員建立群組
        Integer groupId = null;
        Integer displayOrderId = 0;

        if (role == UserRole.GROUP_ADMIN) {
            groupId = 1;
            displayOrderId = 1;
            GroupResponse groupResponse = mock(GroupResponse.class);

            when(groupResponse.groupId())
                    .thenReturn(groupId);

            when(groupService.createGroup(username + "的群組"))
                    .thenReturn(groupResponse);
        }

        // 模擬儲存使用者
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act：執行註冊
        AuthResponse authResponse = authService.register(registerRequest);

        // Assert：驗證回傳結果
        assertNotNull(authResponse);

        verify(userRepository).existsByEmailIgnoreCase(email);
        verify(passwordEncoder).encode(password);

        // 驗證是否建立群組
        if (role == UserRole.GROUP_ADMIN) {
            verify(groupService).createGroup(username + "的群組");
        } else {
            verifyNoInteractions(groupService);
        }

        // 取得傳入save()的使用者資料
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository).save(captor.capture());

        UserEntity savedUser = captor.getValue();

        // 驗證儲存內容
        assertEquals(role, savedUser.getRole());
        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertEquals(passwordHash, savedUser.getPasswordHash());
        assertEquals(groupId, savedUser.getGroupId());
        assertEquals(displayOrderId, savedUser.getDisplayOrderId());
    }

    @Test
    @DisplayName("註冊失敗：Email已被註冊")
    void register_emailAlreadyExists() {
        // 模擬Email已被註冊
        when(userRepository.existsByEmailIgnoreCase(email))
                .thenReturn(true);

        // Act & Assert：執行註冊並驗證例外
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(registerRequest));

        // 驗證HTTP狀態碼
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        // 驗證錯誤訊息
        assertEquals("Email已經被註冊", exception.getReason());

        // 驗證停止註冊功能
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("登入成功")
    void login_success() {
        // 模擬查詢使用者
        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.of(user));

        // 模擬密碼驗證成功
        when(passwordEncoder.matches(password, passwordHash))
                .thenReturn(true);

        // 模擬生成JWT
        when(tokenService.issue(any(Authentication.class), eq(user.getUserId())))
                .thenReturn("mockToken");

        // Act：執行登入
        AuthResponse authResponse = authService.login(loginRequest);

        // Assert：驗證結果
        assertNotNull(authResponse);
        assertEquals(username, authResponse.username());
        assertEquals(email, authResponse.email());
        assertEquals(role, authResponse.role());
        assertEquals("mockToken", authResponse.token());

        verify(userRepository).findByEmailIgnoreCase(email);
        verify(passwordEncoder).matches(password, passwordHash);
        verify(tokenService).issue(any(Authentication.class), eq(user.getUserId()));
    }

    @Test
    @DisplayName("登入失敗：Email尚未被註冊")
    void login_emailNotFound() {
        // 模擬查詢使用者
        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.empty());

        // Act & Assert：執行登入並驗證例外
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(loginRequest));

        // 驗證HTTP狀態碼
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        // 驗證錯誤訊息
        assertEquals("帳號或密碼錯誤", exception.getReason());

        // 驗證沒有執行密碼比對和生成JWT
        verifyNoInteractions(passwordEncoder, tokenService);
    }

    @Test
    @DisplayName("登入失敗：密碼錯誤")
    void login_passwordIncorrect() {
        // 模擬查詢使用者
        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.of(user));

        // 模擬密碼驗證失敗
        when(passwordEncoder.matches(password, passwordHash))
                .thenReturn(false);

        // Act & Assert：執行登入並驗證例外
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(loginRequest));

        // 驗證HTTP狀態碼
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        // 驗證錯誤訊息
        assertEquals("帳號或密碼錯誤", exception.getReason());

        // 驗證有執行密碼比對
        verify(passwordEncoder).matches(password, passwordHash);

        // 驗證沒有生成JWT
        verifyNoInteractions(tokenService);
    }
}
