package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.AdminUpdateUserRequest;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test audit đổi role / khóa-mở tài khoản (sheet T5-06 "API Admin: đổi role, khóa/mở tài khoản → ROLE_CHANGED (before/after);
 * Admin không tự đổi quyền chính mình"; SDD §7 v2 của Hoà).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserServiceAuditTest - Audit đổi role, khóa/mở tài khoản")
class UserServiceAuditTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuditLogger auditLogger;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthService authService;

    @InjectMocks private UserService userService;

    private User admin;
    private User staff;

    private static Role role(String name) {
        Role r = new Role();
        r.setRoleName(name);
        return r;
    }

    private static User user(long id, String username, String roleName) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role(roleName));
        u.setIsActive(true);
        return u;
    }

    @BeforeEach
    void setUp() {
        admin = user(4L, "admin", "ADMIN");
        staff = user(2L, "staff", "IT_STAFF");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(4L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByRoleName("IT_MANAGER")).thenReturn(Optional.of(role("IT_MANAGER")));
        when(roleRepository.findByRoleName("IT_STAFF")).thenReturn(Optional.of(role("IT_STAFF")));
        when(roleRepository.findByRoleName("EMPLOYEE")).thenReturn(Optional.of(role("EMPLOYEE")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PATCH role: ghi ROLE_CHANGED kèm role trước/sau, không còn CHANGE_ROLE")
    void patchRole_logsRoleChangedWithBeforeAfter() {
        userService.changeUserRole(2L, "IT_MANAGER");

        verify(auditLogger).logAfterCommit(eq("ROLE_CHANGED"), eq(4L), eq("ADMIN"), eq("USER"), eq("2"),
                anyString(), eq("SUCCESS"), eq(200),
                eq(Map.of("role", "IT_STAFF")), eq(Map.of("role", "IT_MANAGER")));
        verify(auditLogger, never()).log(eq("CHANGE_ROLE"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PATCH role: Admin tự đổi role mình → 400 CANNOT_CHANGE_OWN_ROLE, không ghi ROLE_CHANGED")
    void patchRole_self_rejected() {
        AppException ex = assertThrows(AppException.class, () -> userService.changeUserRole(4L, "EMPLOYEE"));
        assertEquals(ErrorCode.CANNOT_CHANGE_OWN_ROLE, ex.getErrorCode());
        verify(auditLogger, never()).logAfterCommit(eq("ROLE_CHANGED"), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PUT user đổi role: ghi USER_UPDATED và ROLE_CHANGED kèm before/after")
    void putUser_roleChange_logsRoleChanged() {
        userService.updateUser(2L, new AdminUpdateUserRequest(null, null, null, "EMPLOYEE", null));

        verify(auditLogger).log(eq("USER_UPDATED"), eq(4L), eq("ADMIN"), eq("USER"), eq("2"), anyString(), eq("SUCCESS"), eq(200));
        verify(auditLogger).logAfterCommit(eq("ROLE_CHANGED"), eq(4L), eq("ADMIN"), eq("USER"), eq("2"),
                anyString(), eq("SUCCESS"), eq(200),
                eq(Map.of("role", "IT_STAFF")), eq(Map.of("role", "EMPLOYEE")));
    }

    @Test
    @DisplayName("PUT user không đổi role (chỉ đổi họ tên): không ghi ROLE_CHANGED")
    void putUser_noRoleChange_noRoleChanged() {
        userService.updateUser(2L, new AdminUpdateUserRequest("Tran Thi Binh", null, null, "IT_STAFF", null));

        verify(auditLogger, never()).logAfterCommit(eq("ROLE_CHANGED"), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PUT user: Admin tự đổi role / tự khóa → 400 giống PATCH (không còn 403 FORBIDDEN)")
    void putUser_self_sameErrorsAsPatch() {
        AppException e1 = assertThrows(AppException.class,
                () -> userService.updateUser(4L, new AdminUpdateUserRequest(null, null, null, "EMPLOYEE", null)));
        assertEquals(ErrorCode.CANNOT_CHANGE_OWN_ROLE, e1.getErrorCode());

        AppException e2 = assertThrows(AppException.class,
                () -> userService.updateUser(4L, new AdminUpdateUserRequest(null, null, null, null, false)));
        assertEquals(ErrorCode.CANNOT_LOCK_OWN_ACCOUNT, e2.getErrorCode());
    }

    @Test
    @DisplayName("PATCH status: khóa tài khoản ghi LOCK_USER kèm is_active trước/sau")
    void patchStatus_lock_logsBeforeAfter() {
        userService.changeUserStatus(2L, false);

        verify(auditLogger).logAfterCommit(eq("LOCK_USER"), eq(4L), eq("ADMIN"), eq("USER"), eq("2"),
                eq("LOCK_USER"), eq("SUCCESS"), eq(200),
                eq(Map.of("is_active", true)), eq(Map.of("is_active", false)));
    }
}
