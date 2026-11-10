package com.ojt.p01.dto.response;

/**
 * Kết quả đăng nhập thành công (LoginResponse trong openapi.yaml).
 *
 * @param accessToken JWT dùng cho header Authorization: Bearer <token>
 * @param tokenType luôn là "Bearer"
 * @param expiresIn thời gian sống của token, tính bằng giây (900 = 15 phút)
 * @param user thông tin tài khoản để frontend điều hướng theo role
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
