package com.ojt.p01.security;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Filter chặn mọi request để giải mã JWT trong header Authorization: Bearer <token>.
 * Nạp thông tin Authentication (username + role) vào SecurityContextHolder.
 *
 * Kiểm tra:
 * 1. Chữ ký và hạn dùng JWT (HS256, 15 phút)
 * 2. Token đã logout (Blacklist tra cứu theo jti)
 * 3. Trạng thái tài khoản người dùng (is_active)
 * 4. Thu hồi phiên cũ khi đổi mật khẩu / đổi quyền (BUG-HD-008):
 *    So sánh iat của token với mốc password_changed_at (cắt về giây). Nếu iat trước mốc -> từ chối (401).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenBlacklistRepository tokenBlacklistRepository,
                                   UserRepository userRepository,
                                   AuditLogger auditLogger) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            try {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                String jti = claims.getId();

                // 1. Token đã logout (jti nằm trong token_blacklist) thì không xác thực → 401
                if (!tokenBlacklistRepository.existsByJti(jti)) {
                    String username = claims.get("username", String.class);
                    if (username != null) {
                        User user = userRepository.findByUsername(username).orElse(null);
                        if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
                            boolean validToken = true;

                            // 2. Kiểm tra mốc đổi mật khẩu / đổi vai trò / khóa tài khoản (BUG-HD-008, IA-HD01, ADR-02)
                            OffsetDateTime credentialsUpdatedAt = user.getCredentialsUpdatedAt();
                            if (credentialsUpdatedAt != null) {
                                Date iatDate = claims.getIssuedAt();
                                if (iatDate == null) {
                                    // Fail closed nếu token không có claim iat
                                    validToken = false;
                                    auditLogger.logWithReason("TOKEN_REVOKED", user.getId(),
                                            user.getRole() != null ? user.getRole().getRoleName() : null,
                                            "USER", String.valueOf(user.getId()), "AUTHENTICATE", "FAILURE", 401, "MISSING_IAT");
                                } else {
                                    // BUG-HD-008: token mới có claim iatMs -> so bằng mili-giây để token cũ cùng giây với
                                    // lúc đổi bị 401, token mới cùng giây vẫn hợp lệ.
                                    // Token cũ không có iatMs -> giữ luật cũ: iat < credentials_updated_at (cắt về giây) -> 401.
                                    Object iatMsClaim = claims.get("iatMs");
                                    boolean revoked;
                                    if (iatMsClaim instanceof Number) {
                                        revoked = ((Number) iatMsClaim).longValue() < credentialsUpdatedAt.toInstant().toEpochMilli();
                                    } else {
                                        Instant changedSec = credentialsUpdatedAt.toInstant().truncatedTo(ChronoUnit.SECONDS);
                                        revoked = iatDate.toInstant().isBefore(changedSec);
                                    }
                                    if (revoked) {
                                        validToken = false;
                                        auditLogger.logWithReason("TOKEN_REVOKED", user.getId(),
                                                user.getRole() != null ? user.getRole().getRoleName() : null,
                                                "USER", String.valueOf(user.getId()), "AUTHENTICATE", "FAILURE", 401, "TOKEN_EXPIRED_BY_CREDENTIALS_UPDATE");
                                    }
                                }
                            }

                            if (validToken) {
                                String role = claims.get("role", String.class);
                                List<SimpleGrantedAuthority> authorities = role != null
                                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                        : Collections.emptyList();

                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Token không hợp lệ thì không set Authentication, SecurityContext sẽ unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
