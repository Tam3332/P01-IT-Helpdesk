package com.ojt.p01.service;

import com.ojt.p01.dto.response.DashboardStatsResponse;
import com.ojt.p01.dto.response.SLAMonitorResponse;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ tổng hợp thống kê báo cáo Dashboard (HD14) và Giám sát cảnh báo SLA (HD12).
 * Phân quyền dữ liệu tự động theo vai trò người dùng:
 * - EMPLOYEE: Thống kê và giám sát vé của chính mình
 * - IT_STAFF: Thống kê và giám sát vé được phân công cho mình
 * - IT_MANAGER / ADMIN: Thống kê và giám sát toàn cục hệ thống
 */
@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public DashboardService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/v1/dashboard/stats
     * Thống kê tổng hợp số lượng vé theo trạng thái, vi phạm SLA, danh mục, kỹ thuật viên.
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        User currentUser = getCurrentUser();
        String roleName = (currentUser != null && currentUser.getRole() != null)
                ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        Specification<Ticket> spec = createRoleBasedSpec(currentUser, roleName, false);
        List<Ticket> tickets = ticketRepository.findAll(spec);

        long totalTickets = tickets.size();
        Map<String, Long> byStatus = tickets.stream()
                .filter(t -> t.getStatus() != null)
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));

        Map<String, Long> byCategory = tickets.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getCategoryName() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getCategoryName(), Collectors.counting()));

        Map<String, Long> byAssignee = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCurrentAssignee() != null ? t.getCurrentAssignee().getFullName() : "Chưa phân công",
                        Collectors.counting()
                ));

        long newTickets = byStatus.getOrDefault("NEW", 0L);
        long inProgressTickets = byStatus.getOrDefault("ASSIGNED", 0L)
                + byStatus.getOrDefault("IN_PROGRESS", 0L)
                + byStatus.getOrDefault("REOPEN_PENDING", 0L)
                + byStatus.getOrDefault("REOPENED", 0L);
        long resolvedTickets = byStatus.getOrDefault("RESOLVED", 0L)
                + byStatus.getOrDefault("CLOSED", 0L);

        OffsetDateTime now = OffsetDateTime.now();
        long breachedCount = 0;
        long nearBreachCount = 0;
        List<Double> resolutionHoursList = new ArrayList<>();

        for (Ticket t : tickets) {
            boolean isBreached = Boolean.TRUE.equals(t.getIsSlaBreached());
            if (!isBreached && t.getResolutionDeadline() != null) {
                if ("RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())) {
                    OffsetDateTime finishedAt = t.getResolvedAt() != null ? t.getResolvedAt() : t.getClosedAt();
                    if (finishedAt != null && finishedAt.isAfter(t.getResolutionDeadline())) {
                        isBreached = true;
                    }
                } else {
                    if (now.isAfter(t.getResolutionDeadline())) {
                        isBreached = true;
                    }
                }
            }
            if (isBreached) {
                breachedCount++;
            }

            // Gần hạn: chỉ tính các vé đang mở (chưa Resolved/Closed)
            if (!"RESOLVED".equalsIgnoreCase(t.getStatus()) && !"CLOSED".equalsIgnoreCase(t.getStatus())) {
                if (t.getCreatedAt() != null && t.getResolutionDeadline() != null
                        && t.getResolutionDeadline().isAfter(t.getCreatedAt())) {
                    long totalMillis = Duration.between(t.getCreatedAt(), t.getResolutionDeadline()).toMillis();
                    long elapsedMillis = Duration.between(t.getCreatedAt(), now).toMillis();
                    double pct = totalMillis > 0 ? ((double) elapsedMillis / totalMillis) * 100.0 : 0.0;
                    if (pct >= 80.0 && pct < 100.0 && !isBreached) {
                        nearBreachCount++;
                    }
                }
            }

            // Thời gian xử lý trung bình
            if (t.getCreatedAt() != null && t.getResolvedAt() != null) {
                long minutes = Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes();
                if (minutes >= 0) {
                    resolutionHoursList.add(minutes / 60.0);
                }
            }
        }

        double slaBreachedPct = totalTickets > 0
                ? Math.round(((double) breachedCount / totalTickets) * 1000.0) / 10.0 : 0.0;
        double avgResolutionHours = resolutionHoursList.isEmpty() ? 0.0 :
                Math.round(resolutionHoursList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;

        return new DashboardStatsResponse(
                totalTickets,
                newTickets,
                inProgressTickets,
                resolvedTickets,
                breachedCount,
                slaBreachedPct,
                nearBreachCount,
                avgResolutionHours,
                byStatus,
                byCategory,
                byAssignee
        );
    }

    /**
     * GET /api/v1/dashboard/sla-monitor
     * Danh sách vé cần giám sát thời hạn SLA (HD12).
     * @param breachedOnly true = chỉ lấy vé đã vi phạm; false = lấy toàn bộ vé đang mở có tính % hạn
     */
    @Transactional(readOnly = true)
    public List<SLAMonitorResponse> getSlaMonitor(Boolean breachedOnly) {
        User currentUser = getCurrentUser();
        String roleName = (currentUser != null && currentUser.getRole() != null)
                ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // Chỉ lấy các vé đang mở (chưa hoàn tất giải quyết)
        Specification<Ticket> spec = createRoleBasedSpec(currentUser, roleName, true);
        List<Ticket> openTickets = ticketRepository.findAll(spec);

        OffsetDateTime now = OffsetDateTime.now();
        List<SLAMonitorResponse> items = new ArrayList<>();

        for (Ticket t : openTickets) {
            double pct = 0.0;
            if (t.getCreatedAt() != null && t.getResolutionDeadline() != null
                    && t.getResolutionDeadline().isAfter(t.getCreatedAt())) {
                long totalMillis = Duration.between(t.getCreatedAt(), t.getResolutionDeadline()).toMillis();
                long elapsedMillis = Duration.between(t.getCreatedAt(), now).toMillis();
                pct = totalMillis > 0 ? Math.round(((double) elapsedMillis / totalMillis) * 1000.0) / 10.0 : 0.0;
            }

            boolean isBreached = Boolean.TRUE.equals(t.getIsSlaBreached())
                    || (t.getResolutionDeadline() != null && now.isAfter(t.getResolutionDeadline()))
                    || pct >= 100.0;

            String slaLabel;
            if (isBreached) {
                slaLabel = "BREACHED";
            } else if (pct >= 80.0) {
                slaLabel = "NEAR_BREACH";
            } else {
                slaLabel = "NORMAL";
            }

            if (Boolean.TRUE.equals(breachedOnly) && !isBreached) {
                continue;
            }

            String assigneeName = t.getCurrentAssignee() != null ? t.getCurrentAssignee().getFullName() : null;
            Long assigneeId = t.getCurrentAssignee() != null ? t.getCurrentAssignee().getId() : null;

            items.add(new SLAMonitorResponse(
                    t.getId(),
                    t.getTicketNumber(),
                    t.getTitle(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getResolutionDeadline(),
                    pct,
                    isBreached,
                    assigneeName,
                    assigneeId,
                    slaLabel,
                    t.getCreatedAt()
            ));
        }

        // Sắp xếp mức độ khẩn cấp giảm dần (ưu tiên vé quá hạn, sau đó đến vé sắp quá hạn % cao nhất)
        items.sort((a, b) -> Double.compare(b.slaUsedPct(), a.slaUsedPct()));
        return items;
    }

    private Specification<Ticket> createRoleBasedSpec(User currentUser, String roleName, boolean openOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (openOnly) {
                predicates.add(root.get("status").in(List.of("NEW", "ASSIGNED", "IN_PROGRESS", "REOPENED", "REOPEN_PENDING")));
            }

            if (currentUser != null) {
                if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
                    predicates.add(cb.equal(root.get("requester").get("id"), currentUser.getId()));
                } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
                    predicates.add(cb.equal(root.get("currentAssignee").get("id"), currentUser.getId()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
