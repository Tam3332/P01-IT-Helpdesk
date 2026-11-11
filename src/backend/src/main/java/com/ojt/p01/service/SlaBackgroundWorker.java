package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * SLA Background Worker chạy ngầm định kỳ mỗi 5 phút (Quy trình P2,).
 *
 * Nhiệm vụ:
 * 1. Quét toàn bộ active tickets đang mở: NEW, ASSIGNED, IN_PROGRESS, REOPENED (loại trừ RESOLVED, CLOSED, REOPEN_PENDING).
 * 2. So sánh thời gian hiện tại với Response Deadline và Resolution Deadline.
 * 3. Nếu phát hiện vi phạm và cờ is_sla_breached chưa bật:
 * Bật cờ vĩnh viễn: is_sla_breached = true.
 * Bắn sự kiện Security Audit Log: SLA_BREACHED vào audit_events.jsonl.
 * Ghi nhận vào nhật ký xử lý TicketHistory.
 */
@Component
public class SlaBackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(SlaBackgroundWorker.class);

 /** Các trạng thái mở cần quét tính hạn SLA theo sơ đồ P2 và */
    private static final List<String> ACTIVE_STATUSES = List.of(
            TicketStatus.NEW.name(),
            TicketStatus.ASSIGNED.name(),
            TicketStatus.IN_PROGRESS.name(),
            TicketStatus.REOPENED.name()
    );

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final AuditLogger auditLogger;

    public SlaBackgroundWorker(TicketRepository ticketRepository,
                               TicketHistoryRepository historyRepository,
                               AuditLogger auditLogger) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.auditLogger = auditLogger;
    }

 /**
 * Chạy định kỳ mỗi 5 phút (300.000 ms), bắt đầu sau khi ứng dụng khởi động 10 giây.
 */
    @Scheduled(fixedRateString = "${sla.worker.interval-ms:300000}", initialDelay = 10000)
    public void scheduledSlaScan() {
        try {
            int breachedCount = scanAndCheckSlaBreaches();
            if (breachedCount > 0) {
                log.warn("[SLA Worker] Phát hiện và đánh dấu vi phạm SLA cho {} ticket(s)", breachedCount);
            }
        } catch (Exception e) {
            log.error("[SLA Worker] Lỗi trong phiên quét kiểm tra vi phạm SLA: {}", e.getMessage(), e);
        }
    }

 /**
 * Phương thức quét và kiểm tra vi phạm SLA. Có thể gọi trực tiếp để test hoặc trigger thủ công.
 *
 * @return số lượng ticket mới bị phát hiện vi phạm trong phiên quét này
 */
    @Transactional
    public int scanAndCheckSlaBreaches() {
        List<Ticket> activeTickets = ticketRepository.findByStatusIn(ACTIVE_STATUSES);
        if (activeTickets.isEmpty()) {
            return 0;
        }

        OffsetDateTime now = OffsetDateTime.now();
        int newlyBreachedCount = 0;

        for (Ticket ticket : activeTickets) {
            try {
                boolean isBreached = checkTicketSlaBreach(ticket, now);
                if (isBreached) {
                    newlyBreachedCount++;
                }
            } catch (Exception e) {
                log.error("[SLA Worker] Lỗi kiểm tra SLA cho ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            }
        }

        return newlyBreachedCount;
    }

    private boolean checkTicketSlaBreach(Ticket ticket, OffsetDateTime now) {
        // 1. Kiểm tra vi phạm hạn phản hồi (Response Deadline)
        // Áp dụng khi chưa có phản hồi đầu tiên (first_responded_at == null)
        boolean isResponseBreached = false;
        if (ticket.getFirstRespondedAt() == null && ticket.getResponseDeadline() != null) {
            if (now.isAfter(ticket.getResponseDeadline())) {
                isResponseBreached = true;
            }
        }

        // 2. Kiểm tra vi phạm hạn xử lý (Resolution Deadline)
        boolean isResolutionBreached = false;
        if (ticket.getResolutionDeadline() != null) {
            if (now.isAfter(ticket.getResolutionDeadline())) {
                isResolutionBreached = true;
            }
        }

        // 3. Nếu có vi phạm VÀ cờ is_sla_breached chưa bật -> Bật cờ và ghi log
        if ((isResponseBreached || isResolutionBreached) && !Boolean.TRUE.equals(ticket.getIsSlaBreached())) {
            ticket.setIsSlaBreached(true);
            ticketRepository.save(ticket);

            String breachType = (isResponseBreached && isResolutionBreached)
                    ? "Quá cả hạn phản hồi và hạn xử lý sự cố"
                    : (isResponseBreached ? "Quá hạn phản hồi ban đầu" : "Quá hạn hoàn tất xử lý sự cố");

            // Ghi nhận vào Security Audit Log
            auditLogger.log(
                    "SLA_BREACHED",
                    null,
                    "SYSTEM",
                    "TICKET",
                    ticket.getTicketNumber(),
                    "SLA_MONITOR",
                    "BREACHED",
                    200
            );

            // Ghi nhận vào nhật ký lịch sử TicketHistory
            User historyActor = ticket.getCurrentAssignee() != null ? ticket.getCurrentAssignee() : ticket.getRequester();
            historyRepository.save(new TicketHistory(
                    ticket,
                    historyActor,
                    "SLA_BREACH",
                    "is_sla_breached",
                    "false",
                    "true",
                    "Hệ thống phát hiện vi phạm SLA (" + breachType + ")",
                    "127.0.0.1",
                    now
            ));

            log.warn("[SLA Worker] Ticket {} vi phạm SLA: {}", ticket.getTicketNumber(), breachType);
            return true;
        }

        return false;
    }
}
