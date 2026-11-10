package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.LoginRequest;
import com.ojt.p01.dto.response.LoginResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xác thực (Task C3).
 * Base path: /api/v1/auth
 */
@Tag(name = "Authentication", description = "Đăng nhập, đăng xuất thu hồi token, thông tin người dùng hiện tại")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

 /**
 * POST /api/v1/auth/login
 * API public duy nhất (không cần token) theo .
 * 200: trả JWT | 400: thiếu/sai định dạng | 401: sai email hoặc mật khẩu | 403: tài khoản bị Admin khóa.
 */
    @Operation(
            summary = "Đăng nhập hệ thống",
            description = "Xác thực bằng email và mật khẩu, trả về JWT Access Token hạn 15 phút."
    )
    @SecurityRequirements // Bỏ biểu tượng ổ khóa trên Swagger: API này không cần token
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

 /**
 * GET /api/v1/auth/me (cần token).
 * Frontend gọi khi tải lại trang để biết token còn dùng được và lấy lại thông tin người dùng.
 * 200: UserResponse | 401: thiếu token, token hết hạn hoặc đã logout.
 */
    @Operation(summary = "Thông tin tài khoản hiện tại từ JWT")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser()));
    }

 /**
 * POST /api/v1/auth/logout (cần token).
 * Đưa token đang dùng vào danh sách đen; gọi API khác bằng token này sẽ bị 401.
 * Header Authorization chắc chắn có, vì SecurityConfig đã chặn request không có token hợp lệ.
 */
    @Operation(summary = "Đăng xuất và thu hồi Access Token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.substring("Bearer ".length()));
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}
