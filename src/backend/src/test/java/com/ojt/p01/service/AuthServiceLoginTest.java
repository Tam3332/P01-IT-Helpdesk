package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.LoginRequest;
import com.ojt.p01.dto.response.LoginResponse;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.TokenBlacklist;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import com.ojt.p01.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test đăng nhập và đăng xuất (SRS UC-01, HD01; task T6-06 phần Auth).
 * Lockout đã có ở AuthServiceLockoutTest, file này kiểm các nhánh còn lại của login() và logout().
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // không phải ca nào cũng dùng hết các stub chung
@DisplayName("AuthServiceLoginTest - Đăng nhập và đăng xuất")
class AuthServiceLoginTest {

    private static final String EMAIL = "login.test@helpdesk.local";
    private static final String HASH = "$2b$12$LoginTestHashDemo";
    private static final String RIGHT = "Dung@12345";
    private static final String WRONG = "Sai@12345";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private AuditLogger auditLogger;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setId(1);
        role.setRoleName("EMPLOYEE");
        user = new User();
        user.setId(99L);
        user.setUsername("login.test");
        user.setEmail(EMAIL);
        user.setFullName("Login Test");
        user.setPasswordHash(HASH);
        user.setRole(role);
        user.setIsActive(true);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("khongco@helpdesk.local")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq(RIGHT), eq(HASH))).thenReturn(true);
        when(passwordEncoder.matches(eq(WRONG), anyString())).thenReturn(false);
        when(jwtTokenProvider.generateToken(any())).thenReturn("token-demo");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(900L);
    }

    /** Gọi login, trả mã lỗi (fail test nếu không có lỗi). */
    private ErrorCode loginError(String email, String password) {
        AppException e = assertThrows(AppException.class,
                () -> authService.login(new LoginRequest(email, password)));
        return e.getErrorCode();
    }

    @Test
    @DisplayName("Đúng email + mật khẩu → trả token Bearer 900 giây, ghi LOGIN_SUCCESS 200")
    void rightPassword_returnsToken() {
        LoginResponse res = authService.login(new LoginRequest(EMAIL, RIGHT));

        assertEquals("token-demo", res.accessToken());
        assertEquals("Bearer", res.tokenType());
        assertEquals(900L, res.expiresIn());
        assertEquals(EMAIL, res.user().email());
        verify(auditLogger).log(eq("LOGIN_SUCCESS"), eq(99L), eq("EMPLOYEE"), eq("USER"), eq("99"),
                eq("LOGIN"), eq("SUCCESS"), eq(200));
    }

    @Test
    @DisplayName("Sai mật khẩu → 401 INVALID_CREDENTIALS, ghi LOGIN_FAILED 401, không cấp token")
    void wrongPassword_401() {
        assertEquals(ErrorCode.INVALID_CREDENTIALS, loginError(EMAIL, WRONG));
        assertEquals(401, ErrorCode.INVALID_CREDENTIALS.getHttpStatus().value());
        verify(auditLogger).log(eq("LOGIN_FAILED"), eq(99L), eq("EMPLOYEE"), eq("USER_EMAIL"), eq(EMAIL),
                eq("LOGIN"), eq("FAILURE"), eq(401));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Email không tồn tại → cùng lỗi 401 như sai mật khẩu (không lộ email nào có thật), vẫn chạy so khớp băm")
    void unknownEmail_sameErrorAsWrongPassword() {
        assertEquals(ErrorCode.INVALID_CREDENTIALS, loginError("khongco@helpdesk.local", WRONG));
        // So khớp với băm giả để thời gian phản hồi giống trường hợp email có thật
        verify(passwordEncoder).matches(eq(WRONG), anyString());
        verify(auditLogger).log(eq("LOGIN_FAILED"), isNull(), isNull(), eq("USER_EMAIL"),
                eq("khongco@helpdesk.local"), eq("LOGIN"), eq("FAILURE"), eq(401));
    }

    @Test
    @DisplayName("Tài khoản bị khóa + đúng mật khẩu → 403 ACCOUNT_DISABLED, ghi LOGIN_FAILED 403, không cấp token")
    void disabledAccount_rightPassword_403() {
        user.setIsActive(false);
        assertEquals(ErrorCode.ACCOUNT_DISABLED, loginError(EMAIL, RIGHT));
        assertEquals(403, ErrorCode.ACCOUNT_DISABLED.getHttpStatus().value());
        verify(auditLogger).log(eq("LOGIN_FAILED"), eq(99L), eq("EMPLOYEE"), eq("USER_EMAIL"), eq(EMAIL),
                eq("LOGIN"), eq("FAILURE"), eq(403));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Tài khoản bị khóa + sai mật khẩu → 401 (người không biết mật khẩu không biết tài khoản đang khóa)")
    void disabledAccount_wrongPassword_401() {
        user.setIsActive(false);
        assertEquals(ErrorCode.INVALID_CREDENTIALS, loginError(EMAIL, WRONG));
    }

    @Test
    @DisplayName("Email có chữ hoa và khoảng trắng → chuẩn hóa về chữ thường, đăng nhập thành công")
    void emailNormalized() {
        LoginResponse res = authService.login(new LoginRequest("  Login.Test@Helpdesk.LOCAL ", RIGHT));
        assertEquals("token-demo", res.accessToken());
        verify(userRepository).findByEmail(EMAIL);
    }

    /** Claims giả của token: jti, subject = userId, hạn dùng. */
    private Claims claims(String jti, Instant exp) {
        Claims c = mock(Claims.class);
        when(c.getId()).thenReturn(jti);
        when(c.getSubject()).thenReturn("99");
        when(c.getExpiration()).thenReturn(Date.from(exp));
        return c;
    }

    @Test
    @DisplayName("Logout → lưu jti, userId, hạn token vào token_blacklist")
    void logout_blacklistsToken() {
        Instant exp = Instant.parse("2026-11-25T09:15:00Z");
        Claims c = claims("jti-1", exp); // tạo trước: không gọi when() lồng trong when()
        when(jwtTokenProvider.getClaimsFromToken("token-demo")).thenReturn(c);
        when(tokenBlacklistRepository.existsByJti("jti-1")).thenReturn(false);

        authService.logout("token-demo");

        ArgumentCaptor<TokenBlacklist> saved = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(saved.capture());
        assertEquals("jti-1", saved.getValue().getJti());
        assertEquals(99L, saved.getValue().getUserId());
        assertEquals(exp.atOffset(ZoneOffset.UTC), saved.getValue().getExpiresAt());
    }

    @Test
    @DisplayName("Logout lại token đã thu hồi hoặc token không có jti → không lưu trùng")
    void logout_alreadyRevokedOrNoJti_notSavedAgain() {
        Instant exp = Instant.parse("2026-11-25T09:15:00Z");
        Claims revoked = claims("jti-1", exp);
        Claims noJti = claims(null, exp);
        when(jwtTokenProvider.getClaimsFromToken("token-cu")).thenReturn(revoked);
        when(tokenBlacklistRepository.existsByJti("jti-1")).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken("token-khong-jti")).thenReturn(noJti);

        authService.logout("token-cu");
        authService.logout("token-khong-jti");

        verify(tokenBlacklistRepository, never()).save(any());
    }
}
