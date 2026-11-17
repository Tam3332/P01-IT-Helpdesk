package com.ojt.p01.worker;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Background Worker chạy định kỳ mỗi 5 phút quét giám sát SLA (ADR-04, HD12).
 * Tự động đánh giá:
 * - Gần hạn (>= 80% thời gian)
 * - Quá hạn (> 100% thời gian hoặc now > resolutionDeadline):
 *   + Bật cờ is_sla_breached = true vĩnh viễn (BR-HD-03)
 *   + Ghi TicketHistory để kích hoạt thông báo chuông cho IT Manager & IT Staff phụ trách
 *   + Ghi Security Audit Log sự kiện SLA_BREACHED
 */
@Component
public class SlaBackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(SlaBackgroundWorker.class);

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
     * Chạy định kỳ mỗi 5 phút (300.000 ms), khởi chạy lần đầu sau 10 giây khi server bật.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 10000)
    @Transactional
    public void scanAndCheckSla() {
        log.info("SLA Background Worker: Bắt đầu quét kiểm tra hạn cam kết SLA...");
        List<String> activeStatuses = List.of("NEW", "ASSIGNED", "IN_PROGRESS", "REOPENED", "REOPEN_PENDING");
        List<Ticket> activeTickets = ticketRepository.findByStatusIn(activeStatuses);

        OffsetDateTime now = OffsetDateTime.now();
        int breachedCount = 0;
        int nearBreachCount = 0;

        for (Ticket ticket : activeTickets) {
            if (ticket.getResolutionDeadline() == null || ticket.getCreatedAt() == null) {
                continue;
            }

            long totalDurationMs = Duration.between(ticket.getCreatedAt(), ticket.getResolutionDeadline()).toMillis();
            long elapsedMs = Duration.between(ticket.getCreatedAt(), now).toMillis();
            double pct = totalDurationMs > 0 ? ((double) elapsedMs / totalDurationMs) * 100.0 : 0.0;

            boolean isBreachedNow = now.isAfter(ticket.getResolutionDeadline()) || pct >= 100.0;

            if (isBreachedNow) {
                breachedCount++;
                // Nếu chưa được đánh dấu vi phạm trước đó, kích hoạt đánh dấu và ghi log
                if (!Boolean.TRUE.equals(ticket.getIsSlaBreached())) {
                    ticket.setIsSlaBreached(true);
                    ticket.setUpdatedAt(now);
                    ticketRepository.save(ticket);

                    User actor = ticket.getCurrentAssignee() != null ? ticket.getCurrentAssignee() : ticket.getRequester();
                    Long actorId = actor != null ? actor.getId() : null;
                    String role = (actor != null && actor.getRole() != null) ? actor.getRole().getRoleName() : "SYSTEM";

                    // Ghi vào TicketHistory để hiện thông báo chuông cho Manager & Staff
                    historyRepository.save(new TicketHistory(
                            ticket,
                            actor,
                            "SLA_BREACH",
                            "is_sla_breached",
                            "false",
                            "true",
                            "Vé đã vượt quá thời hạn cam kết giải quyết SLA (Hạn chót: " + ticket.getResolutionDeadline() + ")",
                            "127.0.0.1",
                            now
                    ));

                    // Ghi Security Audit Log sự kiện SLA_BREACHED
                    Map<String, Object> before = Map.of("is_sla_breached", false);
                    Map<String, Object> after = Map.of(
                            "is_sla_breached", true,
                            "resolution_deadline", ticket.getResolutionDeadline().toString(),
                            "elapsed_pct", Math.round(pct * 10.0) / 10.0
                    );
                    auditLogger.log("SLA_BREACHED", actorId, role, "TICKET", ticket.getTicketNumber(),
                            "SLA_CHECK", "BREACHED", 200, before, after);

                    log.warn("SLA Breached detected: Ticket {} đã quá hạn SLA!", ticket.getTicketNumber());
                }
            } else if (pct >= 80.0) {
                nearBreachCount++;
            }
        }

        log.info("SLA Background Worker hoàn tất: quét {} vé hoạt động ({} vé quá hạn, {} vé sắp quá hạn).",
                activeTickets.size(), breachedCount, nearBreachCount);
    }
}
