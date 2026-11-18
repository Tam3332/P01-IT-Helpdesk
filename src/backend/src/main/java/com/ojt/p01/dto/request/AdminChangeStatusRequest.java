package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminChangeStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        Boolean isActive
) {}
