package com.ojt.p01.security;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilterTest - Kiểm thử thu hồi token sau khi đổi mật khẩu / đổi quyền (BUG-HD-008)")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private User testUser;
    private final String rawToken = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        Role role = new Role();
        role.setId(1);
        role.setRoleName("EMPLOYEE");

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("employee.test");
        testUser.setEmail("employee.test@helpdesk.local");
        testUser.setRole(role);
        testUser.setIsActive(true);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + rawToken);
        when(jwtTokenProvider.validateToken(rawToken)).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken(rawToken)).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-12345");
        when(tokenBlacklistRepository.existsByJti("jti-12345")).thenReturn(false);
        when(claims.get("username", String.class)).thenReturn("employee.test");
        when(userRepository.findByUsername("employee.test")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Ca 1: iat trước mốc password_changed_at -> Từ chối (401), SecurityContext null, ghi Audit TOKEN_REVOKED")
    void tokenIssuedBeforePasswordChange_rejected() throws Exception {
        // Mốc đổi mật khẩu lúc T
        Instant changedInstant = Instant.parse("2026-11-23T10:00:00Z");
        testUser.setPasswordChangedAt(changedInstant.atOffset(ZoneOffset.UTC));

        // Token cấp lúc T - 60s
        Instant iatInstant = changedInstant.minusSeconds(60);
        when(claims.getIssuedAt()).thenReturn(Date.from(iatInstant));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Token cấp trước mốc đổi mật khẩu không được gán Authentication");
        verify(filterChain).doFilter(request, response);
        verify(auditLogger).logWithReason(
                eq("TOKEN_REVOKED"),
                eq(10L),
                eq("EMPLOYEE"),
                eq("USER"),
                eq("10"),
                eq("AUTHENTICATE"),
                eq("FAILURE"),
                eq(401),
                eq("TOKEN_EXPIRED_BY_PASSWORD_CHANGE")
        );
    }

    @Test
    @DisplayName("Ca 2a: iat cùng giây với mốc password_changed_at -> Chấp nhận (200), SecurityContext có Authentication")
    void tokenIssuedSameSecondAsPasswordChange_accepted() throws Exception {
        // Mốc đổi mật khẩu có phần mili-giây (vd lúc lưu vào DB)
        Instant changedInstant = Instant.parse("2026-11-23T10:00:00.654Z");
        testUser.setPasswordChangedAt(changedInstant.atOffset(ZoneOffset.UTC));

        // Token cấp trong cùng giây (iat bị cắt mất phần mili-giây nên là .000)
        Instant iatInstant = Instant.parse("2026-11-23T10:00:00.000Z");
        when(claims.getIssuedAt()).thenReturn(Date.from(iatInstant));
        when(claims.get("role", String.class)).thenReturn("EMPLOYEE");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "Token cấp cùng giây với mốc đổi mật khẩu phải hợp lệ");
        assertEquals("employee.test", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
        verify(auditLogger, never()).logWithReason(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Ca 2b: iat sau mốc password_changed_at -> Chấp nhận (200), SecurityContext có Authentication")
    void tokenIssuedAfterPasswordChange_accepted() throws Exception {
        Instant changedInstant = Instant.parse("2026-11-23T10:00:00Z");
        testUser.setPasswordChangedAt(changedInstant.atOffset(ZoneOffset.UTC));

        // Token mới cấp sau khi đổi mật khẩu 5 giây
        Instant iatInstant = changedInstant.plusSeconds(5);
        when(claims.getIssuedAt()).thenReturn(Date.from(iatInstant));
        when(claims.get("role", String.class)).thenReturn("EMPLOYEE");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("employee.test", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Ca 3: Mốc password_changed_at null (chưa từng đổi) -> Chấp nhận (200), SecurityContext có Authentication")
    void passwordChangedAtNull_accepted() throws Exception {
        testUser.setPasswordChangedAt(null);
        when(claims.get("role", String.class)).thenReturn("EMPLOYEE");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("employee.test", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Ca 4: Token thiếu claim iat khi có mốc đổi mật khẩu -> Fail closed (401), ghi Audit MISSING_IAT")
    void tokenMissingIat_failClosed() throws Exception {
        Instant changedInstant = Instant.parse("2026-11-23T10:00:00Z");
        testUser.setPasswordChangedAt(changedInstant.atOffset(ZoneOffset.UTC));

        when(claims.getIssuedAt()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Token thiếu claim iat phải bị từ chối theo nguyên tắc fail-closed");
        verify(filterChain).doFilter(request, response);
        verify(auditLogger).logWithReason(
                eq("TOKEN_REVOKED"),
                eq(10L),
                eq("EMPLOYEE"),
                eq("USER"),
                eq("10"),
                eq("AUTHENTICATE"),
                eq("FAILURE"),
                eq(401),
                eq("MISSING_IAT")
        );
    }

    @Test
    @DisplayName("Ca 5: Token nằm trong Blacklist (đã logout) -> Không gán Authentication")
    void tokenBlacklisted_rejected() throws Exception {
        when(tokenBlacklistRepository.existsByJti("jti-12345")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
