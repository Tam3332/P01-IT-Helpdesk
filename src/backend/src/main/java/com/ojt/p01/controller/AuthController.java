package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.LoginRequest;
import com.ojt.p01.dto.response.LoginResponse;
import com.ojt.p01.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xác thực (Task C3, SRS HD01, UC-01).
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
     * API public duy nhất (không cần token) theo SDD §6.2.
     * 200: trả JWT | 400: thiếu/sai định dạng | 401: sai email hoặc mật khẩu | 403: tài khoản bị Admin khóa.
     */
    @Operation(
            summary = "Đăng nhập hệ thống (HD01, UC-01)",
            description = "Xác thực bằng email và mật khẩu, trả về JWT Access Token hạn 15 phút."
    )
    @SecurityRequirements // Bỏ biểu tượng ổ khóa trên Swagger: API này không cần token
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }
}
