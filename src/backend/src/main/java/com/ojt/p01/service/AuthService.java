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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Logic đăng nhập (Task C2).
 *
 * Thứ tự kiểm tra:
 * 1. Tìm user theo email → không có: 401 "Email hoặc mật khẩu không đúng"
 * 2. So mật khẩu với hash BCrypt → sai: 401 cùng thông báo (không nói rõ sai trường nào)
 * 3. Tài khoản bị Admin khóa → từ chối, 403
 * 4. Hợp lệ → cấp JWT 15 phút
 *
 * Đếm số lần sai, khóa 15 phút, HTTP 429 khi đăng nhập thất bại liên tiếp.
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

    // Giới hạn đổi mật khẩu: tối đa 3 lần/ngày mỗi tài khoản (bảo vệ tài khoản, chống brute-force / lạm dụng)
    private static final int MAX_PASSWORD_CHANGES_PER_DAY = 3;
    private final Map<Long, List<OffsetDateTime>> passwordChangeTracker = new ConcurrentHashMap<>();

    // Lockout (SRS UC-01 2b, IA-HD05): sai 5 lần trong 5 phút theo email + IP → khóa 15 phút, HTTP 429.
    // Lưu trong bộ nhớ (quyết định 17/11): restart backend thì mất bộ đếm (ghi trong SDD §6).
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration FAIL_WINDOW = Duration.ofMinutes(5);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    // Tên event khi bắt đầu khóa: ACCOUNT_LOCKED theo sheet T5-06 (Hoà chốt 17/11; trước đây là BRUTE_FORCE_DETECTED). Chỉ ghi 1 lần lúc bắt đầu khóa, các lần thử trong lúc khóa chỉ ghi LOGIN_FAILED 429
    static final String LOCK_EVENT = "ACCOUNT_LOCKED";
    // Quá ngưỡng này thì dọn bản ghi đã hết hạn, tránh phình bộ nhớ khi bị gửi hàng loạt email ngẫu nhiên
    private static final int CLEANUP_THRESHOLD = 1000;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    // Tách đồng hồ để unit test giả lập thời gian trôi (không đổi constructor → test cũ vẫn chạy)
    Clock clock = Clock.systemUTC();

    /** Các lần sai gần đây của một cặp email + IP và thời điểm hết khóa. */
    private static final class LoginAttempt {
        final Deque<Instant> failures = new ArrayDeque<>();
        Instant lockedUntil;
    }

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
        // Đếm theo email + IP nguồn (UC-01 2b). Email không tồn tại cũng đếm → không lộ email nào có thật
        String attemptKey = email + "|" + clientIp();
        Instant now = clock.instant();

        // 0. Đang bị khóa → chặn trước khi kiểm mật khẩu (nhập đúng mật khẩu vẫn bị chặn, SEC-TC-HD-02)
        if (isLocked(attemptKey, now)) {
            auditLogger.log("LOGIN_FAILED", null, null, "USER_EMAIL", email, "LOGIN", "FAILURE", 429);
            throw new AppException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            // Email không tồn tại: chưa biết actor → ghi email đã nhập để Monitoring đếm theo email + IP
            throw loginFailure(attemptKey, email, null, null, now);
        }
        User user = found.get();

        // Kiểm tra mật khẩu TRƯỚC trạng thái tài khoản: người không biết mật khẩu
        // sẽ không biết được tài khoản đó có tồn tại hay đang bị khóa.
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw loginFailure(attemptKey, email, user.getId(), roleName, now);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            auditLogger.log("LOGIN_FAILED", user.getId(), roleName, "USER_EMAIL", email, "LOGIN", "FAILURE", 403);
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Đăng nhập đúng → xóa bộ đếm (chuỗi sai "liên tiếp" bị ngắt)
        loginAttempts.remove(attemptKey);

        String token = jwtTokenProvider.generateToken(user);
        auditLogger.log("LOGIN_SUCCESS", user.getId(), roleName, "USER", String.valueOf(user.getId()), "LOGIN", "SUCCESS", 200);
        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds(), UserResponse.from(user));
    }

    /**
     * Ghi 1 lần đăng nhập sai và trả lỗi cần ném.
     * Lần sai thứ 5 trong 5 phút → khóa 15 phút, ghi LOCK_EVENT, trả 429; các lần trước trả 401.
     */
    private AppException loginFailure(String key, String email, Long actorId, String role, Instant now) {
        boolean lockedNow = registerFailure(key, now);
        auditLogger.log("LOGIN_FAILED", actorId, role, "USER_EMAIL", email, "LOGIN", "FAILURE", lockedNow ? 429 : 401);
        if (lockedNow) {
            auditLogger.log(LOCK_EVENT, actorId, role, "USER_EMAIL", email, "LOGIN", "FAILURE", 429);
            return new AppException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
        return new AppException(ErrorCode.INVALID_CREDENTIALS);
    }

    /** Thêm 1 lần sai; trả true nếu lần này chạm ngưỡng và vừa khóa. */
    private boolean registerFailure(String key, Instant now) {
        if (loginAttempts.size() > CLEANUP_THRESHOLD) {
            cleanupExpired(now);
        }
        LoginAttempt a = loginAttempts.computeIfAbsent(key, k -> new LoginAttempt());
        synchronized (a) {
            // Bỏ các lần sai đã nằm ngoài cửa sổ 5 phút
            Instant windowStart = now.minus(FAIL_WINDOW);
            while (!a.failures.isEmpty() && !a.failures.peekFirst().isAfter(windowStart)) {
                a.failures.pollFirst();
            }
            a.failures.addLast(now);
            if (a.failures.size() >= MAX_FAILED_ATTEMPTS) {
                a.lockedUntil = now.plus(LOCK_DURATION);
                a.failures.clear();
                return true;
            }
            return false;
        }
    }

    /** Đang trong thời gian khóa? Hết 15 phút thì tự mở khóa. */
    private boolean isLocked(String key, Instant now) {
        LoginAttempt a = loginAttempts.get(key);
        if (a == null) {
            return false;
        }
        synchronized (a) {
            if (a.lockedUntil == null) {
                return false;
            }
            if (now.isBefore(a.lockedUntil)) {
                return true;
            }
            a.lockedUntil = null;
            return false;
        }
    }

    /** Xóa các cặp email + IP đã hết khóa và không còn lần sai nào trong 5 phút gần nhất. */
    private void cleanupExpired(Instant now) {
        Instant windowStart = now.minus(FAIL_WINDOW);
        loginAttempts.entrySet().removeIf(e -> {
            LoginAttempt a = e.getValue();
            synchronized (a) {
                boolean lockOver = a.lockedUntil == null || !now.isBefore(a.lockedUntil);
                boolean noRecentFail = a.failures.isEmpty() || !a.failures.peekLast().isAfter(windowStart);
                return lockOver && noRecentFail;
            }
        });
    }

    /** IP nguồn của request (sau Nginx, Tomcat đọc X-Forwarded-For nhờ forward-headers-strategy: native). */
    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            return req.getRemoteAddr();
        }
        return "unknown"; // unit test không có request
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

        // 0. Kiểm tra số lần đổi mật khẩu trong ngày hôm nay (tối đa 3 lần/ngày)
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        List<OffsetDateTime> history = passwordChangeTracker.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>());
        long countToday = history.stream().filter(t -> !t.isBefore(startOfDay)).count();
        if (countToday >= MAX_PASSWORD_CHANGES_PER_DAY) {
            auditLogger.log("PASSWORD_CHANGE_LIMIT_EXCEEDED", user.getId(), roleName, "USER", String.valueOf(user.getId()), "CHANGE_PASSWORD", "FAILURE", 429);
            throw new AppException(ErrorCode.PASSWORD_CHANGE_LIMIT_EXCEEDED);
        }

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

        // 3. Cập nhật mật khẩu mới băm BCrypt, cập nhật mốc password_changed_at và tắt cờ yêu cầu đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        // Ghi nhận lần đổi mật khẩu thành công vào tracker trong ngày
        history.add(now);

        // 4. Ghi Audit Log an toàn
        auditLogger.log("PASSWORD_CHANGED", user.getId(), roleName, "USER", String.valueOf(user.getId()), "CHANGE_PASSWORD", "SUCCESS", 200);
    }

    /**
     * Quản trị viên đặt lại mật khẩu cho người dùng -> xóa giới hạn đổi mật khẩu trong ngày của người dùng đó.
     */
    public void resetPasswordChangeLimit(Long userId) {
        if (userId != null) {
            passwordChangeTracker.remove(userId);
        }
    }
}
