package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.ReopenTicketRequest;
import com.ojt.p01.dto.response.ReopenRequestResponse;
import com.ojt.p01.entity.ReopenRequest;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.ReopenRequestRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Gửi yêu cầu mở lại ticket và danh sách yêu cầu cho IT Manager (HD10, UC-08 Nhánh B bước 1-5, BR-HD-04).
 * Phạm vi: BPMN P3 từ "Is the incident resolution accepted?" tới
 * "Review the Reopen Reason & Compare Against the Previous Resolution".
 * Duyệt / từ chối (POST /reopen-requests/{id}/approve | reject) chưa nằm trong phần này.
 */
@Service
public class ReopenService {

    /** BR-HD-04: chỉ mở lại trong 07 ngày (168 giờ) kể từ lúc Resolved. */
    private static final Duration REOPEN_WINDOW = Duration.ofHours(168);
    private static final int MIN_REASON_LENGTH = 20;
    private static final Set<String> REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final TicketRepository ticketRepository;
    private final ReopenRequestRepository reopenRequestRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final TicketStatusTransitionValidator transitionValidator;
    private final AuditLogger auditLogger;

    public ReopenService(TicketRepository ticketRepository,
                         ReopenRequestRepository reopenRequestRepository,
                         TicketHistoryRepository historyRepository,
                         UserRepository userRepository,
                         TicketStatusTransitionValidator transitionValidator,
                         AuditLogger auditLogger) {
        this.ticketRepository = ticketRepository;
        this.reopenRequestRepository = reopenRequestRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.transitionValidator = transitionValidator;
        this.auditLogger = auditLogger;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tickets/{id}/reopen — Employee chủ ticket gửi yêu cầu mở lại
    // -------------------------------------------------------------------------

    /**
     * Thứ tự kiểm tra:
     * 1. Role khác EMPLOYEE đã bị chặn 403 ở SecurityConfig (SRS UC-08: Admin/IT Staff gọi API mở lại → 403).
     * 2. Ticket không tồn tại → 404 (không ghi audit, SDD §7).
     * 3. Employee không phải chủ ticket → 404 + AUTHZ_DENIED (IA-HD02, Anti-IDOR).
     * 4. Lý do sau khi trim < 20 ký tự → 400 VALIDATION_FAILED (BR-HD-04).
     * 5. Không ở RESOLVED / quá 168 giờ / đã có yêu cầu PENDING → 422 (SDD §4 dòng /reopen).
     */
    @Transactional
    public ReopenRequestResponse requestReopen(String idOrNumber, ReopenTicketRequest request, String ipAddress) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        if (!ticket.getRequester().getId().equals(currentUser.getId())) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "REOPEN_REQUEST", 404);
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

        // @Size(min = 20) chưa loại khoảng trắng đầu/cuối nên kiểm lại sau khi trim
        String reason = request.reopenReason() == null ? "" : request.reopenReason().trim();
        if (reason.length() < MIN_REASON_LENGTH) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Lý do mở lại phải có ít nhất 20 ký tự");
        }

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (currentStatus != TicketStatus.RESOLVED) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ có thể yêu cầu mở lại ticket đang ở trạng thái RESOLVED");
        }

        OffsetDateTime now = OffsetDateTime.now();
        // resolvedAt trống thì không xác định được mốc 7 ngày → coi như không đủ điều kiện
        if (ticket.getResolvedAt() == null
                || Duration.between(ticket.getResolvedAt(), now).compareTo(REOPEN_WINDOW) > 0) {
            throw new AppException(ErrorCode.REOPEN_WINDOW_EXPIRED);
        }

        if (reopenRequestRepository.existsByTicket_IdAndStatus(ticket.getId(), ReopenRequest.STATUS_PENDING)) {
            throw new AppException(ErrorCode.REOPEN_ALREADY_PENDING);
        }

        transitionValidator.assertTransitionAllowed(currentStatus, TicketStatus.REOPEN_PENDING);

        ReopenRequest saved;
        try {
            // saveAndFlush để vi phạm uq_ticket_pending_reopen (2 request gửi cùng lúc) báo lỗi ngay tại đây
            saved = reopenRequestRepository.saveAndFlush(new ReopenRequest(ticket, currentUser, reason, now));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.REOPEN_ALREADY_PENDING);
        }

        // reopened_at và reopen_count để bước IT Manager duyệt cập nhật:
        // hạn xử lý mới tính từ lúc duyệt (SRS BR-HD-03), ticket chỉ sang Reopened sau khi duyệt (BR-HD-04)
        ticket.setStatus(TicketStatus.REOPEN_PENDING.name());
        ticket.setReopenReason(reason);
        ticketRepository.save(ticket);

        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "REOPEN_REQUEST",
                "status",
                TicketStatus.RESOLVED.name(),
                TicketStatus.REOPEN_PENDING.name(),
                reason,
                clientIp,
                now
        ));

        // Audit REOPEN_REQUESTED (SDD §7) chỉ ghi sau khi transaction commit thành công
        Long actorId = currentUser.getId();
        String ticketNumber = ticket.getTicketNumber();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditLogger.log("REOPEN_REQUESTED", actorId, roleName, "TICKET", ticketNumber,
                        "REOPEN_REQUEST", "SUCCESS", 201);
            }
        });

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reopen-requests?status=PENDING — IT Manager (SecurityConfig chặn role khác 403)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReopenRequestResponse> getReopenRequests(String status) {
        String normalized = (status == null || status.isBlank()) ? ReopenRequest.STATUS_PENDING : status.trim().toUpperCase();
        if (!REQUEST_STATUSES.contains(normalized)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "status chỉ nhận PENDING, APPROVED hoặc REJECTED");
        }
        return reopenRequestRepository.findByStatusOrderByCreatedAtAsc(normalized)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ReopenRequestResponse toResponse(ReopenRequest r) {
        Ticket t = r.getTicket();
        return new ReopenRequestResponse(
                r.getId(),
                t.getId(),
                t.getTicketNumber(),
                t.getTitle(),
                r.getRequester().getId(),
                r.getRequester().getFullName(),
                r.getReopenReason(),
                t.getResolutionNote(),
                r.getStatus(),
                r.getReviewedBy() != null ? r.getReviewedBy().getId() : null,
                r.getReviewNote(),
                r.getCreatedAt(),
                r.getReviewedAt()
        );
    }

    /** Giống TicketService: nhận ID số hoặc mã HD-YYYY-XXXX. */
    private Ticket findTicketByIdOrNumber(String idOrNumber) {
        if (idOrNumber == null || idOrNumber.isBlank()) {
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }
        String key = idOrNumber.trim();
        try {
            Long id = Long.parseLong(key);
            return ticketRepository.findById(id)
                    .orElseGet(() -> ticketRepository.findByTicketNumber(key)
                            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND)));
        } catch (NumberFormatException ignored) {
            return ticketRepository.findByTicketNumber(key)
                    .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
