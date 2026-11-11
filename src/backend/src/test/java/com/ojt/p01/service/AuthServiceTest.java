package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.ChangePasswordRequest;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import com.ojt.p01.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceTest - Kiểm thử chức năng Đổi mật khẩu")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(1);
        role.setRoleName("EMPLOYEE");

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("test.employee");
        testUser.setEmail("test@helpdesk.local");
        testUser.setPasswordHash("$2b$12$OldPasswordHashDemo");
        testUser.setRole(role);
        testUser.setIsActive(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test.employee",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công: mật khẩu cũ đúng, mật khẩu mới khác mật khẩu cũ")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456");

        when(userRepository.findByUsername("test.employee")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@123", "$2b$12$OldPasswordHashDemo")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "$2b$12$OldPasswordHashDemo")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("$2b$12$NewPasswordHashDemo");

        assertDoesNotThrow(() -> authService.changePassword(request));

        assertEquals("$2b$12$NewPasswordHashDemo", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
        verify(auditLogger).log(eq("PASSWORD_CHANGED"), eq(10L), eq("EMPLOYEE"), eq("USER"), eq("10"), eq("CHANGE_PASSWORD"), eq("SUCCESS"), eq(200));
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại: mật khẩu hiện tại không đúng -> ném INVALID_CURRENT_PASSWORD (400)")
    void changePassword_wrongCurrentPassword_throwsError() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass@123", "NewPass@456");

        when(userRepository.findByUsername("test.employee")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass@123", "$2b$12$OldPasswordHashDemo")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, ex.getErrorCode());
        assertEquals(400, ex.getErrorCode().getHttpStatus().value());

        verify(userRepository, never()).save(any());
        verify(auditLogger).log(eq("PASSWORD_CHANGE_FAILED"), eq(10L), eq("EMPLOYEE"), eq("USER"), eq("10"), eq("CHANGE_PASSWORD"), eq("FAILURE"), eq(400));
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại: mật khẩu mới trùng mật khẩu cũ -> ném PASSWORD_SAME_AS_CURRENT (400)")
    void changePassword_sameAsCurrentPassword_throwsError() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "OldPass@123");

        when(userRepository.findByUsername("test.employee")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@123", "$2b$12$OldPasswordHashDemo")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.PASSWORD_SAME_AS_CURRENT, ex.getErrorCode());
        assertEquals(400, ex.getErrorCode().getHttpStatus().value());

        verify(userRepository, never()).save(any());
        verify(auditLogger).log(eq("PASSWORD_CHANGE_FAILED"), eq(10L), eq("EMPLOYEE"), eq("USER"), eq("10"), eq("CHANGE_PASSWORD"), eq("FAILURE"), eq(400));
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại: chưa đăng nhập -> ném UNAUTHENTICATED (401)")
    void changePassword_unauthenticated_throwsError() {
        SecurityContextHolder.clearContext();
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456");

        AppException ex = assertThrows(AppException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }
}
