package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminChangeRoleRequest(
        @NotBlank(message = "Vai trò không được để trống")
        String role
) {}
