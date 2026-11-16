package com.ojt.p01.dto.request;

public record AdminUpdateUserRequest(
        String fullName,
        String phone,
        String department,
        String role,
        Boolean isActive
) {}
