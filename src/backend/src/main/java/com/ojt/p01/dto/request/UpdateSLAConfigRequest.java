package com.ojt.p01.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO chỉnh sửa cấu hình SLA (PUT /api/v1/admin/sla-configs/{priority}).
 * BR-HD-03: giờ liên tục (không phải giờ làm việc), phải > 0.
 * Quyền: ADMIN only.
 */
public record UpdateSLAConfigRequest(

        @NotNull(message = "Thời gian phản hồi không được để trống")
        @DecimalMin(value = "0.01", message = "Thời gian phản hồi phải lớn hơn 0")
        BigDecimal responseTimeHours,

        @NotNull(message = "Thời gian giải quyết không được để trống")
        @DecimalMin(value = "0.01", message = "Thời gian giải quyết phải lớn hơn 0")
        BigDecimal resolutionTimeHours,

        @NotNull(message = "Ngưỡng cảnh báo không được để trống")
        Integer warningThresholdPct,

        @NotNull(message = "Ngưỡng vi phạm không được để trống")
        Integer breachThresholdPct
) {}
