package com.ojt.p01.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO trả về cấu hình SLA (GET + PUT /api/v1/admin/sla-configs).
 */
public record SLAConfigResponse(
        Integer id,
        String priorityLevel,
        BigDecimal responseTimeHours,
        BigDecimal resolutionTimeHours,
        Integer warningThresholdPct,
        Integer breachThresholdPct,
        OffsetDateTime updatedAt
) {}
