package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO đại diện cho 1 bản ghi vé cần giám sát SLA (HD12).
 * Phục vụ màn hình Giám sát SLA và API GET /api/v1/dashboard/sla-monitor.
 */
public record SLAMonitorResponse(
        Long ticketId,
        String ticketNumber,
        String title,
        String priority,
        String status,
        OffsetDateTime resolutionDeadline,
        Double slaUsedPct,
        Boolean isSlaBreached,
        String assigneeName,
        Long currentAssigneeId,
        String slaLabel, // "NORMAL" (< 80%), "NEAR_BREACH" (80% - 100%), "BREACHED" (> 100%)
        OffsetDateTime createdAt
) {}
