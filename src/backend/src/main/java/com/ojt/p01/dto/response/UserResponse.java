package com.ojt.p01.dto.response;

import com.ojt.p01.entity.User;

/**
 * Thông tin tài khoản trả về cho client (UserResponse trong openapi.yaml).
 * Không chứa password_hash, failed_login_attempts, locked_until.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        String department,
        Boolean isActive
) {

 /** Chuyển Entity User sang DTO, chỉ lấy các trường được phép lộ ra ngoài. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null ? user.getRole().getRoleName() : null,
                user.getDepartment(),
                user.getIsActive()
        );
    }
}
