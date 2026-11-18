package com.ojt.p01.dto.response;

import java.util.Map;

/**
 * DTO dữ liệu thống kê tổng hợp cho Dashboard (HD14).
 * Tự động lọc phạm vi theo vai trò người gọi:
 * - EMPLOYEE: Thống kê các vé do chính mình gửi
 * - IT_STAFF: Thống kê các vé được phân công cho mình
 * - IT_MANAGER / ADMIN: Thống kê toàn cục hệ thống
 */
public record DashboardStatsResponse(
        long totalTickets,
        long newTickets,
        long inProgressTickets,
        long resolvedTickets,
        long slaBreachedCount,
        double slaBreachedPct,
        long nearBreachCount,
        double avgResolutionHours,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byAssignee
) {}
