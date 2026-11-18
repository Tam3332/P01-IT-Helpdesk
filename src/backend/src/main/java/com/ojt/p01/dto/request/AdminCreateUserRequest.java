package com.ojt.p01.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 50, message = "Tên đăng nhập không được vượt quá 50 ký tự")
        String username,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        String fullName,

        String department,

        @NotBlank(message = "Vai trò không được để trống")
        String role
) {}
