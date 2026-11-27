package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.RoleRepository;
import com.ojt.p01.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test BUG-HD-012 (Medium, 26/11): Admin đặt lại mật khẩu không còn mật khẩu mặc định cố định trong mã nguồn.
 * Để trống / ngắn / quá dài → 400 VALIDATION_FAILED, không đổi gì; hợp lệ → lưu băm, bắt đổi mật khẩu, thu hồi token cũ.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // không phải ca nào cũng dùng hết các stub chung
@DisplayName("UserServiceResetPasswordTest - Admin đặt lại mật khẩu (BUG-HD-012)")
class UserServiceResetPasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuditLogger auditLogger;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthService authService;

    @InjectMocks private UserService userService;

    private User admin;
    private User staff;

    private static User user(long id, String username, String roleName) {
        Role r = new Role();
        r.setRoleName(roleName);
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setFullName(username);
        u.setRole(r);
        u.setIsActive(true);
        u.setPasswordHash("hash-cu");
        u.setMustChangePassword(false);
        return u;
    }

    @BeforeEach
    void setUp() {
        admin = user(4L, "admin", "ADMIN");
        staff = user(2L, "staff", "IT_STAFF");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(4L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "bcrypt(" + inv.getArgument(0) + ")");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Gọi reset và kiểm 400 VALIDATION_FAILED, mật khẩu và trạng thái tài khoản không đổi. */
    private void assertRejected(String newPassword) {
        AppException e = assertThrows(AppException.class, () -> userService.resetUserPassword(2L, newPassword));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.getErrorCode());
        assertEquals(400, ErrorCode.VALIDATION_FAILED.getHttpStatus().value());
        assertEquals("hash-cu", staff.getPasswordHash());
        assertFalse(staff.getMustChangePassword());
        assertNull(staff.getCredentialsUpdatedAt());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(authService, never()).resetPasswordChangeLimit(anyLong());
    }

    @Test
    @DisplayName("Không gửi mật khẩu (body rỗng) → 400, không dùng mật khẩu mặc định")
    void nullPassword_rejected() {
        assertRejected(null);
    }

    @Test
    @DisplayName("Mật khẩu trống hoặc chỉ khoảng trắng → 400")
    void blankPassword_rejected() {
        assertRejected("   ");
    }

    @Test
    @DisplayName("Mật khẩu 7 ký tự → 400 (tối thiểu 8, khớp đổi mật khẩu)")
    void tooShort_rejected() {
        assertRejected("Abc@123");
    }

    @Test
    @DisplayName("Mật khẩu 101 ký tự → 400 (tối đa 100)")
    void tooLong_rejected() {
        assertRejected("A".repeat(101));
    }

    @Test
    @DisplayName("Mật khẩu hợp lệ → lưu băm đúng giá trị Admin nhập, bắt đổi mật khẩu, thu hồi token cũ, ghi audit")
    void validPassword_resetsAndForcesChange() {
        userService.resetUserPassword(2L, "  Tam@2026x  ");

        verify(passwordEncoder).encode("Tam@2026x"); // bỏ khoảng trắng đầu/cuối, không thay bằng giá trị khác
        assertEquals("bcrypt(Tam@2026x)", staff.getPasswordHash());
        assertTrue(staff.getMustChangePassword());
        assertNotNull(staff.getCredentialsUpdatedAt()); // token cấp trước mốc này bị từ chối (BUG-HD-008)
        verify(authService).resetPasswordChangeLimit(2L);
        verify(auditLogger).log(eq("ADMIN_RESET_PASSWORD"), eq(4L), eq("ADMIN"), eq("USER"), eq("2"),
                eq("RESET_PASSWORD"), eq("SUCCESS"), eq(200));
    }

    @Test
    @DisplayName("Admin tự đặt lại mật khẩu của mình → 403, không đổi gì")
    void self_forbidden() {
        AppException e = assertThrows(AppException.class, () -> userService.resetUserPassword(4L, "Tam@2026x"));
        assertEquals(ErrorCode.FORBIDDEN, e.getErrorCode());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
