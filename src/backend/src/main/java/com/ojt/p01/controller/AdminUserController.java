package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.AdminChangeRoleRequest;
import com.ojt.p01.dto.request.AdminChangeStatusRequest;
import com.ojt.p01.dto.request.AdminCreateUserRequest;
import com.ojt.p01.dto.request.AdminResetPasswordRequest;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị tài khoản người dùng (Admin only).
 * Base path: /api/v1/admin/users
 *
 * Phân quyền: Spring Security tự động chặn các role khác (403 FORBIDDEN + AUTHZ_DENIED).
 */
@Tag(name = "Admin & Users", description = "Quản trị người dùng hệ thống (Admin only)")
@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/admin/users
     * Lấy danh sách tài khoản người dùng có phân trang.
     */
    @Operation(summary = "Danh sách tài khoản người dùng (ADMIN)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    /**
     * POST /api/v1/admin/users
     * Tạo tài khoản mới.
     */
    @Operation(summary = "Tạo tài khoản mới (ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công", response));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/role
     * Gán lại vai trò cho tài khoản.
     */
    @Operation(summary = "Gán lại vai trò cho tài khoản (ADMIN)")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminChangeRoleRequest request
    ) {
        UserResponse response = userService.changeUserRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.success("Đổi vai trò thành công", response));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/status
     * Kích hoạt hoặc khóa tài khoản.
     */
    @Operation(summary = "Kích hoạt hoặc khóa tài khoản (ADMIN)")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminChangeStatusRequest request
    ) {
        UserResponse response = userService.changeUserStatus(userId, request.isActive());
        String msg = Boolean.TRUE.equals(request.isActive()) ? "Kích hoạt tài khoản thành công" : "Khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    /**
     * POST /api/v1/admin/users/{userId}/reset-password
     * Đặt lại mật khẩu cho người dùng khi quên mật khẩu và kích hoạt cờ mustChangePassword.
     */
    @Operation(summary = "Đặt lại mật khẩu cho người dùng (ADMIN)")
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(
            @PathVariable Long userId,
            @RequestBody(required = false) AdminResetPasswordRequest request
    ) {
        String newPass = request != null ? request.newPassword() : null;
        UserResponse response = userService.resetUserPassword(userId, newPass);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công. Người dùng sẽ phải đổi mật khẩu khi đăng nhập.", response));
    }
}
