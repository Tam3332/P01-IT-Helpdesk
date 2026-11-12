package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.ChangePasswordRequest;
import com.ojt.p01.dto.request.LoginRequest;
import com.ojt.p01.dto.response.LoginResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.entity.TokenBlacklist;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import com.ojt.p01.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

/**
 * Logic đăng nhập (Task C2).
 *
 * Thứ tự kiểm tra:
 * 1. Tìm user theo email → không có: 401 "Email hoặc mật khẩu không đúng"
 * 2. So mật khẩu với hash BCrypt → sai: 401 cùng thông báo (không nói rõ sai trường nào)
 * 3. Tài khoản bị Admin khóa → từ chối, 403
 * 4. Hợp lệ → cấp JWT 15 phút
 *
 * Chưa làm trong tuần 3 (theo file phân công): đếm số lần sai, khóa 15 phút, HTTP 429 → tuần 5.
 * Mỗi lần đăng nhập ghi 1 dòng audit LOGIN_SUCCESS / LOGIN_FAILED. Không ghi mật khẩu.
 */
@Service
public class AuthService {

 /*
 * Hash BCrypt (cost 12) của một chuỗi ngẫu nhiên, không thuộc tài khoản nào.
 * Khi email không tồn tại vẫn chạy BCrypt với hash này để thời gian phản hồi
 * gần bằng trường hợp sai mật khẩu → kẻ tấn công khó đoán email nào có thật.
 */
    private static final String DUMMY_HASH =
            "$2b$12$gLmxogPMpfdQi1/FvfnB3ePHKmjWKk93/7IAPDLHzAI61Y/xedzzO";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final AuditLogger auditLogger;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenBlacklistRepository tokenBlacklistRepository,
                       AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Chuẩn hóa email: bỏ khoảng trắng đầu/cuối, chữ thường (email trong DB đang lưu chữ thường)
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            // Email không tồn tại: chưa biết actor → ghi email đã nhập để Monitoring đếm theo email + IP
            auditLogger.log("LOGIN_FAILED", null, null, "USER_EMAIL", email, "LOGIN", "FAILURE", 401);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();

        // Kiểm tra mật khẩu TRƯỚC trạng thái tài khoản: người không biết mật khẩu
        // sẽ không biết được tài khoản đó có tồn tại hay đang bị khóa.
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogger.log("LOGIN_FAILED", user.getId(), roleName, "USER_EMAIL", email, "LOGIN", "FAILURE", 401);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            auditLogger.log("LOGIN_FAILED", user.getId(), roleName, "USER_EMAIL", email, "LOGIN", "FAILURE", 403);
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        String token = jwtTokenProvider.generateToken(user);
        auditLogger.log("LOGIN_SUCCESS", user.getId(), roleName, "USER", String.valueOf(user.getId()), "LOGIN", "SUCCESS", 200);
        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds(), UserResponse.from(user));
    }

 /**
 * GET /api/v1/auth/me: thông tin người dùng đang đăng nhập.
 * Lấy username từ SecurityContext (do JwtAuthenticationFilter nạp từ token đã kiểm chữ ký)
 * rồi đọc lại từ DB để trả đúng dữ liệu hiện tại (tên, phòng ban, role).
 */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        return UserResponse.from(user);
    }

 /**
 * POST /api/v1/auth/logout: thu hồi token đang dùng.
 * Lưu jti vào token_blacklist; từ đó JwtAuthenticationFilter từ chối token này (→ 401).
 * Gọi lại logout bằng cùng token sẽ bị 401 ngay ở filter, nên không ghi trùng.
 */
    @Transactional
    public void logout(String token) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        String jti = claims.getId();
        if (jti == null || tokenBlacklistRepository.existsByJti(jti)) {
            return;
        }
        Long userId = Long.valueOf(claims.getSubject());
        OffsetDateTime expiresAt = claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);
        tokenBlacklistRepository.save(new TokenBlacklist(jti, userId, expiresAt));
    }

    /**
     * POST /api/v1/auth/change-password: Đổi mật khẩu cho người dùng đang đăng nhập.
     * Quy tắc:
     * 1. Xác thực người dùng hiện tại từ SecurityContext (nếu chưa đăng nhập -> 401 UNAUTHENTICATED)
     * 2. Kiểm tra mật khẩu hiện tại (so khớp BCrypt) -> nếu sai: 400 INVALID_CURRENT_PASSWORD (không trả 401 tránh văng FE)
     * 3. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ -> nếu trùng: 400 PASSWORD_SAME_AS_CURRENT
     * 4. Băm mật khẩu mới bằng BCrypt (cost 12) và lưu CSDL
     * 5. Ghi Audit Log PASSWORD_CHANGED (không ghi mật khẩu)
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        // 1. So khớp mật khẩu hiện tại
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditLogger.log("PASSWORD_CHANGE_FAILED", user.getId(), roleName, "USER", String.valueOf(user.getId()), "CHANGE_PASSWORD", "FAILURE", 400);
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 2. Mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            auditLogger.log("PASSWORD_CHANGE_FAILED", user.getId(), roleName, "USER", String.valueOf(user.getId()), "CHANGE_PASSWORD", "FAILURE", 400);
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        // 3. Cập nhật mật khẩu mới băm BCrypt và tắt cờ yêu cầu đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // 4. Ghi Audit Log an toàn
        auditLogger.log("PASSWORD_CHANGED", user.getId(), roleName, "USER", String.valueOf(user.getId()), "CHANGE_PASSWORD", "SUCCESS", 200);
    }
}
