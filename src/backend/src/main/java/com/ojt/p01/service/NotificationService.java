package com.ojt.p01.service;

import com.ojt.p01.dto.response.AuditLogResponse;
import com.ojt.p01.dto.response.NotificationResponse;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public NotificationService(TicketRepository ticketRepository,
                               TicketHistoryRepository historyRepository,
                               UserRepository userRepository,
                               AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

 /**
 * Lấy danh sách thông báo chính xác theo từng vai trò:
 * 1. ADMIN (Phương án 1): Giám sát toàn cục: Cảnh báo vi phạm SLA, sự cố nghiêm trọng (Critical/High) và an ninh kiểm toán.
 * 2. IT_MANAGER: Hiện thông báo khi có ticket MỚI (NEW) cần phân công hoặc mở lại (REOPENED, REOPEN_PENDING).
 * 3. IT_STAFF: Chỉ hiện ticket khi Manager phân công cho mình (ASSIGNED).
 * 4. EMPLOYEE: Nhận biến động vé: Phân công (ASSIGNED), Đang xử lý, Chờ duyệt mở lại, Đã giải quyết, Đóng.
 */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(int limit) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        List<NotificationResponse> result = new ArrayList<>();

        if ("ADMIN".equalsIgnoreCase(roleName)) {
            // Admin: Cảnh báo sự cố cấp cao (Critical/High), vi phạm SLA và sự kiện an ninh hệ thống
            return getNotificationsForAdmin(safeLimit);

        } else if ("IT_MANAGER".equalsIgnoreCase(roleName)) {
            // Manager: Nhận các thông báo quan trọng trong vận hành từ TicketHistory:
            // 1. Yêu cầu mở lại (REOPEN_REQUEST) - Cần phê duyệt
            // 2. Ticket mới tạo (CREATE) - Cần điều phối kỹ thuật viên
            // 3. Phê duyệt hoặc từ chối mở lại (REOPEN_APPROVE, REOPEN_REJECT)
            List<TicketHistory> managerHistories = historyRepository.findNotificationsForManager(PageRequest.of(0, safeLimit));
            for (TicketHistory h : managerHistories) {
                Ticket t = h.getTicket();
                String action = h.getAction();
                String status;
                if ("REOPEN_REQUEST".equals(action)) {
                    status = "REOPEN_PENDING";
                } else if ("REOPEN_APPROVE".equals(action)) {
                    status = "REOPENED";
                } else if ("REOPEN_REJECT".equals(action)) {
                    status = "REOPEN_REJECTED";
                } else if ("CREATE".equals(action)) {
                    status = "NEW";
                } else if ("SLA_BREACH".equals(action)) {
                    status = "BREACHED";
                } else {
                    status = (h.getNewValue() != null && !h.getNewValue().isBlank()) ? h.getNewValue() : t.getStatus();
                }

                result.add(new NotificationResponse(
                        h.getId(), // ID sự kiện duy nhất trong TicketHistory, đảm bảo mỗi lần có sự kiện mới chuông đều báo unread
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        status,
                        action,
                        h.getActor() != null ? h.getActor().getFullName() : (t.getRequester() != null ? t.getRequester().getFullName() : "Nhân viên"),
                        h.getTimestamp()
                ));
            }

            // Bổ sung các ticket vi phạm SLA (nếu có)
            try {
                List<Ticket> breached = ticketRepository.findNotificationsForAdmin(PageRequest.of(0, 5));
                for (Ticket bt : breached) {
                    if (Boolean.TRUE.equals(bt.getIsSlaBreached()) && !"RESOLVED".equals(bt.getStatus()) && !"CLOSED".equals(bt.getStatus())) {
                        long breachId = bt.getId() * 100000000L + (bt.getUpdatedAt() != null ? bt.getUpdatedAt().toEpochSecond() % 100000000L : 0L);
                        result.add(new NotificationResponse(
                                breachId,
                                bt.getId(),
                                bt.getTicketNumber(),
                                bt.getTitle(),
                                "BREACHED",
                                "SLA_BREACH",
                                bt.getRequester() != null ? bt.getRequester().getFullName() : "Hệ thống",
                                bt.getUpdatedAt() != null ? bt.getUpdatedAt() : bt.getCreatedAt()
                        ));
                    }
                }
            } catch (Exception ignored) {}

            result.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
            if (result.size() > safeLimit) {
                result = new ArrayList<>(result.subList(0, safeLimit));
            }

        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            // Staff: Chỉ hiện ticket khi Manager phân công cho mình hoặc cảnh báo vi phạm SLA
            List<TicketHistory> staffHistories = historyRepository.findNotificationsForStaff(
                    currentUser.getId(), String.valueOf(currentUser.getId()), PageRequest.of(0, safeLimit));

            for (TicketHistory h : staffHistories) {
                Ticket t = h.getTicket();
                String notifStatus = "SLA_BREACH".equals(h.getAction()) ? "BREACHED" : "ASSIGNED";
                String notifTitle = "SLA_BREACH".equals(h.getAction()) ? "Cảnh báo: Vé đã quá hạn cam kết SLA!" : t.getTitle();
                result.add(new NotificationResponse(
                        h.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        notifTitle,
                        notifStatus,
                        h.getAction(),
                        h.getActor() != null ? h.getActor().getFullName() : "IT Manager",
                        h.getTimestamp()
                ));
            }

        } else {
            // Employee: Nhận thông báo khi vé có biến động quan trọng:
            // - Phân công kỹ thuật viên (ASSIGNED)
            // - Kỹ thuật viên tiếp nhận xử lý (IN_PROGRESS)
            // - Chờ quản lý duyệt mở lại (REOPEN_PENDING)
            // - Duyệt mở lại vé (REOPENED)
            // - Hoàn tất giải quyết (RESOLVED)
            // - Đóng vé (CLOSED)
            List<TicketHistory> empHistories = historyRepository.findNotificationsForEmployee(
                    currentUser.getId(), PageRequest.of(0, safeLimit));

            for (TicketHistory h : empHistories) {
                Ticket t = h.getTicket();
                String status;
                if ("current_assignee_id".equals(h.getFieldChanged())) {
                    status = "ASSIGNED";
                } else if ("CREATE".equals(h.getAction())) {
                    status = "NEW";
                } else if ("REOPEN_REJECT".equals(h.getAction())) {
                    status = "REOPEN_REJECTED";
                } else if ("REOPEN_APPROVE".equals(h.getAction())) {
                    status = "REOPEN_APPROVED";
                } else {
                    status = (h.getNewValue() != null && !h.getNewValue().isBlank()) ? h.getNewValue() : t.getStatus();
                }

                String actorName = h.getActor() != null ? h.getActor().getFullName() : "Hệ thống";
                String action = h.getAction() != null ? h.getAction() : "STATUS_CHANGE";

                result.add(new NotificationResponse(
                        h.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        status,
                        action,
                        actorName,
                        h.getTimestamp()
                ));
            }
        }

        return result;
    }

 /**
 * Admin: Giám sát toàn cục hệ thống:
 * 1. Sự cố mức độ cao (Critical / High)
 * 2. Vé vi phạm cam kết thời hạn SLA (SLA Breach)
 * 3. Sự kiện an ninh & kiểm toán (Anti-IDOR Authz Denied, Đăng nhập thất bại, Quản lý tài khoản)
 */
    private List<NotificationResponse> getNotificationsForAdmin(int safeLimit) {
        List<NotificationResponse> list = new ArrayList<>();

        // 1. Lấy các ticket vi phạm SLA hoặc mức ưu tiên CRITICAL / HIGH
        try {
            List<Ticket> adminTickets = ticketRepository.findNotificationsForAdmin(PageRequest.of(0, safeLimit));
            for (Ticket t : adminTickets) {
                String action;
                String status;
                if (Boolean.TRUE.equals(t.getIsSlaBreached())) {
                    action = "SLA_BREACH";
                    status = "BREACHED";
                } else {
                    action = "CRITICAL_ALERT";
                    status = t.getPriority() != null ? t.getPriority() : "HIGH";
                }

                list.add(new NotificationResponse(
                        t.getId(),
                        t.getId(),
                        t.getTicketNumber(),
                        t.getTitle(),
                        status,
                        action,
                        t.getRequester() != null ? t.getRequester().getFullName() : "Hệ thống",
                        t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt()
                ));
            }
        } catch (Exception ignored) {
        }

        // 2. Lấy các sự kiện an ninh & kiểm toán từ AuditLogService
        try {
            PageResponse<AuditLogResponse> logsPage = auditLogService.getAuditLogs(0, safeLimit, null, null);
            if (logsPage != null && logsPage.content() != null) {
                for (AuditLogResponse log : logsPage.content()) {
                    String action = log.action();
                    String eventType = action != null ? action.toUpperCase() : "";
                    String outcome = log.outcome() != null ? log.outcome() : "";
                    int httpStatus = log.httpStatus() != null ? log.httpStatus() : 200;

                    boolean isSecurityWarning = eventType.contains("AUTHZ_DENIED")
                            || eventType.contains("LOGIN_FAILED")
                            || "DENIED".equalsIgnoreCase(outcome)
                            || "FAILURE".equalsIgnoreCase(outcome)
                            || httpStatus == 401 || httpStatus == 403;

                    boolean isUserMgmt = eventType.contains("USER_CREATED")
                            || eventType.contains("USER_LOCKED")
                            || eventType.contains("PASSWORD_RESET")
                            || eventType.contains("ADMIN_RESET_PASSWORD")
                            || eventType.contains("ROLE_ASSIGNED");

                    if (isSecurityWarning || isUserMgmt) {
                        String ticketNumber;
                        String title;
                        String status;
                        if (isSecurityWarning) {
                            ticketNumber = eventType.contains("LOGIN") ? "SEC-AUTH" : "SEC-ALERT";
                            title = eventType.contains("LOGIN")
                                    ? "Cảnh báo đăng nhập thất bại (IP: " + (log.srcIp() != null ? log.srcIp() : "Unknown") + ")"
                                    : "Phát hiện truy cập trái phép bị chặn (Anti-IDOR: " + (log.targetId() != null ? log.targetId() : "Tài nguyên") + ")";
                            status = "BREACHED";
                        } else {
                            ticketNumber = "USER-MGMT";
                            title = "Thay đổi quản trị tài khoản người dùng (" + (log.targetId() != null ? "#" + log.targetId() : "User") + ")";
                            status = "ASSIGNED";
                        }

                        OffsetDateTime ts = OffsetDateTime.now();
                        if (log.timestamp() != null) {
                            try {
                                ts = OffsetDateTime.parse(log.timestamp());
                            } catch (Exception ignored) {
                            }
                        }

                        list.add(new NotificationResponse(
                                log.id() != null ? log.id() : (long) Math.abs(Objects.hash(log.correlationId(), log.timestamp())),
                                null,
                                ticketNumber,
                                title,
                                status,
                                eventType,
                                log.actorName() != null ? log.actorName() : (log.actor() != null ? log.actor() : "Hệ thống"),
                                ts
                        ));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Sắp xếp toàn bộ theo thời gian mới nhất lên đầu và lấy an toàn số lượng limit
        list.sort((a, b) -> {
            if (a.timestamp() == null && b.timestamp() == null) return 0;
            if (a.timestamp() == null) return 1;
            if (b.timestamp() == null) return -1;
            return b.timestamp().compareTo(a.timestamp());
        });

        if (list.size() > safeLimit) {
            return list.subList(0, safeLimit);
        }
        return list;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String principal = auth.getName();
            return userRepository.findByUsername(principal)
                    .orElseGet(() -> userRepository.findByEmail(principal)
                            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED)));
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
