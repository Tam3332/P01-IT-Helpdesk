package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu đổi mật khẩu (POST /api/v1/auth/change-password).
 * Áp dụng ẩn mật khẩu khỏi toString() để chống rò rỉ vào log (chuẩn an toàn thông tin IA).
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
        String newPassword

) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
