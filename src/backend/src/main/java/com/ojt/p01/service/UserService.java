package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.AdminCreateUserRequest;
import com.ojt.p01.dto.request.AdminUpdateUserRequest;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.RoleRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Nghiệp vụ quản lý tài khoản người dùng và quản trị Admin.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogger auditLogger;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       AuditLogger auditLogger,
                       PasswordEncoder passwordEncoder,
                       AuthService authService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogger = auditLogger;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /**
     * Danh sách IT Staff đang hoạt động để IT Manager chọn khi phân công.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveItStaff() {
        return userRepository.findByRole_RoleNameAndIsActiveTrueOrderByFullNameAsc("IT_STAFF")
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Lấy toàn bộ danh sách tài khoản người dùng có phân trang (Admin).
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize, Sort.by("id").ascending());
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserResponse> content = userPage.getContent().stream()
                .map(UserResponse::from)
                .toList();
        return new PageResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    /**
     * Tạo tài khoản người dùng mới (Admin only).
     */
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL);
        }

        Role role = roleRepository.findByRoleName(request.role().trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setDepartment(request.department() != null ? request.department().trim() : null);
        user.setRole(role);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);

        User saved = userRepository.save(user);

        User currentUser = getCurrentUser();
        auditLogger.log("CREATE_USER", currentUser != null ? currentUser.getId() : null, "ADMIN", "USER",
                String.valueOf(saved.getId()), "CREATE_USER", "SUCCESS", 201);

        return UserResponse.from(saved);
    }

    /**
     * Gán lại vai trò cho tài khoản (Admin only).
     */
    @Transactional
    public UserResponse changeUserRole(Long userId, String newRoleName) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Role newRole = roleRepository.findByRoleName(newRoleName.trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        String oldRole = user.getRole() != null ? user.getRole().getRoleName() : "NONE";
        user.setRole(newRole);
        User updated = userRepository.save(user);

        // Audit ROLE_CHANGED kèm role trước/sau, ghi sau commit (SDD §7, IA-HD01, IA-HD04; sheet T5-06)
        logRoleChanged(currentUser, user.getId(), oldRole, newRole.getRoleName());

        return UserResponse.from(updated);
    }

    /**
     * Kích hoạt hoặc khóa tài khoản (Admin only).
     */
    @Transactional
    public UserResponse changeUserStatus(Long userId, Boolean isActive) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId) && Boolean.FALSE.equals(isActive)) {
            throw new AppException(ErrorCode.CANNOT_LOCK_OWN_ACCOUNT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Boolean oldActive = user.getIsActive();
        user.setIsActive(isActive);
        User updated = userRepository.save(user);

        // Audit khóa/mở tài khoản kèm is_active trước/sau, ghi sau commit (sheet T5-06)
        logActiveChanged(currentUser, user.getId(), oldActive, isActive);

        return UserResponse.from(updated);
    }

    /**
     * Đặt lại mật khẩu cho tài khoản người dùng và yêu cầu đổi mật khẩu ở lần đăng nhập tiếp theo (Admin only).
     */
    @Transactional
    public UserResponse resetUserPassword(Long userId, String newPassword) {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Quản trị viên vui lòng đổi mật khẩu tại trang Cá nhân / Đổi mật khẩu");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String passToSet = (newPassword != null && !newPassword.trim().isBlank()) ? newPassword.trim() : "Demo@12345";
        if (passToSet.length() < 6) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Mật khẩu đặt lại phải có ít nhất 6 ký tự");
        }

        user.setPasswordHash(passwordEncoder.encode(passToSet));
        user.setMustChangePassword(true);
        User updated = userRepository.save(user);

        // Giải phóng giới hạn đổi mật khẩu trong ngày cho user
        authService.resetPasswordChangeLimit(userId);

        auditLogger.log("ADMIN_RESET_PASSWORD", currentUser != null ? currentUser.getId() : null, "ADMIN", "USER",
                String.valueOf(user.getId()), "RESET_PASSWORD", "SUCCESS", 200);

        return UserResponse.from(updated);
    }

    /**
     * Cập nhật thông tin chi tiết người dùng (Admin only).
     */
    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User currentUser = getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Giữ giá trị cũ để ghi before/after nếu PUT đổi role hoặc trạng thái
        String oldRole = user.getRole() != null ? user.getRole().getRoleName() : "NONE";
        Boolean oldActive = user.getIsActive();
        boolean isSelf = currentUser != null && currentUser.getId().equals(userId);

        if (request != null) {
            if (request.fullName() != null && !request.fullName().trim().isBlank()) {
                user.setFullName(request.fullName().trim());
            }
            if (request.phone() != null) {
                user.setPhone(request.phone().trim());
            }
            if (request.department() != null) {
                user.setDepartment(request.department().trim());
            }
            if (request.role() != null && !request.role().trim().isBlank()) {
                // Admin tự đổi role của mình: 400 giống PATCH /role (thống nhất mã lỗi)
                if (isSelf && !request.role().trim().equalsIgnoreCase(oldRole)) {
                    throw new AppException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
                }
                Role role = roleRepository.findByRoleName(request.role().trim().toUpperCase())
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                user.setRole(role);
            }
            if (request.isActive() != null) {
                // Admin tự khóa mình: 400 giống PATCH /status
                if (isSelf && Boolean.FALSE.equals(request.isActive())) {
                    throw new AppException(ErrorCode.CANNOT_LOCK_OWN_ACCOUNT);
                }
                user.setIsActive(request.isActive());
            }
        }

        user.setUpdatedAt(OffsetDateTime.now());
        User updated = userRepository.save(user);

        auditLogger.log("USER_UPDATED", currentUser != null ? currentUser.getId() : null, "ADMIN", "USER",
                String.valueOf(user.getId()), "UPDATE", "SUCCESS", 200);

        // PUT có đổi role hoặc trạng thái → ghi thêm event riêng kèm before/after như PATCH
        String newRole = updated.getRole() != null ? updated.getRole().getRoleName() : "NONE";
        if (!newRole.equals(oldRole)) {
            logRoleChanged(currentUser, user.getId(), oldRole, newRole);
        }
        if (request != null && request.isActive() != null && !request.isActive().equals(oldActive)) {
            logActiveChanged(currentUser, user.getId(), oldActive, request.isActive());
        }

        return UserResponse.from(updated);
    }

    /** ROLE_CHANGED: before/after là role cũ/mới (SDD §7 v2). */
    private void logRoleChanged(User admin, Long targetUserId, String oldRole, String newRole) {
        auditLogger.logAfterCommit("ROLE_CHANGED", admin != null ? admin.getId() : null, "ADMIN", "USER",
                String.valueOf(targetUserId), "ROLE_CHANGE", "SUCCESS", 200,
                java.util.Map.of("role", oldRole), java.util.Map.of("role", newRole));
    }

    /** LOCK_USER / ACTIVATE_USER: before/after là is_active cũ/mới. */
    private void logActiveChanged(User admin, Long targetUserId, Boolean oldActive, Boolean newActive) {
        String action = Boolean.TRUE.equals(newActive) ? "ACTIVATE_USER" : "LOCK_USER";
        java.util.Map<String, Object> before = new java.util.HashMap<>();
        before.put("is_active", oldActive);
        java.util.Map<String, Object> after = new java.util.HashMap<>();
        after.put("is_active", newActive);
        auditLogger.logAfterCommit(action, admin != null ? admin.getId() : null, "ADMIN", "USER",
                String.valueOf(targetUserId), action, "SUCCESS", 200, before, after);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
