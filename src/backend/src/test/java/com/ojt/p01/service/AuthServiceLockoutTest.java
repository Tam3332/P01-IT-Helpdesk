package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.LoginRequest;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import com.ojt.p01.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test lockout đăng nhập (SRS UC-01 2b, IA-HD05; task T5-06):
 * sai 5 lần trong 5 phút theo email + IP → khóa 15 phút, HTTP 429.
 * Unit test không có HTTP request nên IP = "unknown" cho mọi lần gọi; khóa thực tế = email.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceLockoutTest - Khóa tạm khi đăng nhập sai nhiều lần")
class AuthServiceLockoutTest {

    private static final String EMAIL = "lock.test@helpdesk.local";
    private static final String HASH = "$2b$12$LockoutTestHashDemo";
    private static final String RIGHT = "Dung@12345";
    private static final String WRONG = "Sai@12345";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private AuditLogger auditLogger;

    @InjectMocks
    private AuthService authService;

    /** Đồng hồ giả: test tự cho thời gian trôi bằng advance(). */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-17T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        authService.clock = clock;

        Role role = new Role();
        role.setId(1);
        role.setRoleName("EMPLOYEE");
        User user = new User();
        user.setId(99L);
        user.setUsername("lock.test");
        user.setEmail(EMAIL);
        user.setPasswordHash(HASH);
        user.setRole(role);
        user.setIsActive(true);

        // lenient: không phải test nào cũng dùng hết các stub này
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findByEmail("khongco@helpdesk.local")).thenReturn(Optional.empty());
        lenient().when(passwordEncoder.matches(eq(RIGHT), eq(HASH))).thenReturn(true);
        lenient().when(passwordEncoder.matches(eq(WRONG), anyString())).thenReturn(false);
        lenient().when(jwtTokenProvider.generateToken(any())).thenReturn("token-demo");
        lenient().when(jwtTokenProvider.getExpirationSeconds()).thenReturn(900L);
    }

    /** Gọi login và trả mã lỗi (null nếu đăng nhập thành công). */
    private ErrorCode attempt(String email, String password) {
        try {
            authService.login(new LoginRequest(email, password));
            return null;
        } catch (AppException e) {
            return e.getErrorCode();
        }
    }

    @Test
    @DisplayName("Sai 4 lần → 401; lần 5 → 429 và ghi BRUTE_FORCE_DETECTED đúng 1 lần")
    void fifthFailure_locksAndReturns429() {
        for (int i = 1; i <= 4; i++) {
            assertEquals(ErrorCode.INVALID_CREDENTIALS, attempt(EMAIL, WRONG), "lần " + i);
        }
        assertEquals(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, attempt(EMAIL, WRONG));
        assertEquals(429, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS.getHttpStatus().value());
        verify(auditLogger, times(1)).log(eq(AuthService.LOCK_EVENT), eq(99L), eq("EMPLOYEE"),
                eq("USER_EMAIL"), eq(EMAIL), eq("LOGIN"), eq("FAILURE"), eq(429));
    }

    @Test
    @DisplayName("Đang khóa: nhập đúng mật khẩu vẫn 429, không cấp token")
    void locked_rightPasswordStillBlocked() {
        for (int i = 0; i < 5; i++) attempt(EMAIL, WRONG);
        clock.advance(Duration.ofMinutes(14));
        assertEquals(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, attempt(EMAIL, RIGHT));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Hết 15 phút → tự mở khóa, đăng nhập đúng thành công")
    void afterLockDuration_unlocks() {
        for (int i = 0; i < 5; i++) attempt(EMAIL, WRONG);
        clock.advance(Duration.ofMinutes(15));
        assertNull(attempt(EMAIL, RIGHT));
        verify(jwtTokenProvider, times(1)).generateToken(any());
    }

    @Test
    @DisplayName("Các lần sai cách nhau quá 5 phút không cộng dồn")
    void failuresOutsideWindow_notCounted() {
        for (int i = 0; i < 4; i++) attempt(EMAIL, WRONG);
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        // 4 lần cũ đã ra ngoài cửa sổ 5 phút → lần này chỉ là lần thứ 1
        assertEquals(ErrorCode.INVALID_CREDENTIALS, attempt(EMAIL, WRONG));
        for (int i = 0; i < 3; i++) {
            assertEquals(ErrorCode.INVALID_CREDENTIALS, attempt(EMAIL, WRONG));
        }
        assertEquals(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, attempt(EMAIL, WRONG));
    }

    @Test
    @DisplayName("Đăng nhập đúng giữa chừng → xóa bộ đếm (chuỗi sai không còn liên tiếp)")
    void successResetsCounter() {
        for (int i = 0; i < 4; i++) attempt(EMAIL, WRONG);
        assertNull(attempt(EMAIL, RIGHT));
        for (int i = 0; i < 4; i++) {
            assertEquals(ErrorCode.INVALID_CREDENTIALS, attempt(EMAIL, WRONG));
        }
    }

    @Test
    @DisplayName("Email không tồn tại cũng bị khóa ở lần 5 → không lộ email nào có thật")
    void unknownEmail_lockedSameWay() {
        for (int i = 0; i < 4; i++) {
            assertEquals(ErrorCode.INVALID_CREDENTIALS, attempt("khongco@helpdesk.local", WRONG));
        }
        assertEquals(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, attempt("khongco@helpdesk.local", WRONG));
    }

    @Test
    @DisplayName("Khóa email này không ảnh hưởng email khác")
    void lockIsPerEmail() {
        for (int i = 0; i < 5; i++) attempt("khongco@helpdesk.local", WRONG);
        assertNull(attempt(EMAIL, RIGHT));
    }
}
