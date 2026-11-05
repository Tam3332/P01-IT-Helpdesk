package com.ojt.p01.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body đăng nhập (POST /api/v1/auth/login, UC-01).
 * Đăng nhập bằng email + mật khẩu (SRS HD01). Thiếu hoặc sai định dạng → HTTP 400.
 */
public record LoginRequest(

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        String password

) {}
