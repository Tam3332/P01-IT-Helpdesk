package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.ApproveReopenRequest;
import com.ojt.p01.dto.request.RejectReopenRequest;
import com.ojt.p01.dto.request.ReopenTicketRequest;
import com.ojt.p01.dto.response.ReopenRequestResponse;
import com.ojt.p01.entity.*;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.*;
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
 * Quản lý quy trình mở lại ticket (HD10, BPMN P3, BR-HD-02, BR-HD-03, BR-HD-04).
 * - Employee gửi yêu cầu mở lại (POST /tickets/{id}/reopen)
 * - IT Manager xem danh sách (GET /reopen-requests)
 * - IT Manager phê duyệt (POST /reopen-requests/{id}/approve)
 * - IT Manager từ chối (POST /reopen-requests/{id}/reject)
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
    private final SLAConfigRepository slaConfigRepository;
    private final TicketAssignmentRepository assignmentRepository;

    public ReopenService(TicketRepository ticketRepository,
                         ReopenRequestRepository reopenRequestRepository,
                         TicketHistoryRepository historyRepository,
                         UserRepository userRepository,
                         TicketStatusTransitionValidator transitionValidator,
                         AuditLogger auditLogger,
                         SLAConfigRepository slaConfigRepository,
                         TicketAssignmentRepository assignmentRepository) {
        this.ticketRepository = ticketRepository;
        this.reopenRequestRepository = reopenRequestRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.transitionValidator = transitionValidator;
        this.auditLogger = auditLogger;
        this.slaConfigRepository = slaConfigRepository;
        this.assignmentRepository = assignmentRepository;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tickets/{id}/reopen — Employee chủ ticket gửi yêu cầu mở lại
    // -------------------------------------------------------------------------

    @Transactional
    public ReopenRequestResponse requestReopen(String idOrNumber, ReopenTicketRequest request, String ipAddress) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        if (!ticket.getRequester().getId().equals(currentUser.getId())) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "REOPEN_REQUEST", 404);
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

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
            saved = reopenRequestRepository.saveAndFlush(new ReopenRequest(ticket, currentUser, reason, now));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.REOPEN_ALREADY_PENDING);
        }

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
    // GET /api/v1/reopen-requests?status=PENDING — IT Manager
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
    // POST /api/v1/reopen-requests/{id}/approve — IT Manager phê duyệt mở lại (BPMN P3)
    // -------------------------------------------------------------------------

    @Transactional
    public ReopenRequestResponse approveReopenRequest(Long requestId, ApproveReopenRequest request, String ipAddress) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        ReopenRequest reopenRequest = reopenRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu mở lại ticket"));

        if (!ReopenRequest.STATUS_PENDING.equalsIgnoreCase(reopenRequest.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Yêu cầu mở lại này đã được xử lý trước đó (trạng thái: " + reopenRequest.getStatus() + ")");
        }

        Ticket ticket = reopenRequest.getTicket();
        TicketStatus currentStatus;
        try {
            currentStatus = TicketStatus.valueOf(ticket.getStatus());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (currentStatus != TicketStatus.REOPEN_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Ticket không ở trạng thái chờ duyệt mở lại (hiện tại: " + ticket.getStatus() + ")");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        String reviewNote = (request != null && request.reviewNote() != null) ? request.reviewNote().trim() : null;

        // 1. Cập nhật ReopenRequest sang APPROVED
        reopenRequest.setStatus(ReopenRequest.STATUS_APPROVED);
        reopenRequest.setReviewedBy(currentUser);
        reopenRequest.setReviewNote(reviewNote);
        reopenRequest.setReviewedAt(now);
        reopenRequestRepository.save(reopenRequest);

        // 2. Kích hoạt hạn SLA mới (SRS BR-HD-03: hạn xử lý mới tính từ lúc IT Manager duyệt mở lại; không tính lại hạn phản hồi)
        SLAConfig sla = slaConfigRepository.findByPriorityLevel(ticket.getPriority())
                .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));
        long resolutionMinutes = Math.round(sla.getResolutionTimeHours().doubleValue() * 60.0);
        ticket.setResolutionDeadline(now.plusMinutes(resolutionMinutes));

        // 3. Cập nhật mốc thời gian Reopen và tăng reopen_count
        ticket.setReopenedAt(now);
        int currentCount = ticket.getReopenCount() == null ? 0 : ticket.getReopenCount();
        ticket.setReopenCount(currentCount + 1);

        // 4. Xử lý phân công lại (Reassign to New IT Staff?) theo sơ đồ BPMN P3
        boolean hasNewAssignee = request != null && request.newAssigneeId() != null;
        User newAssignee = null;
        if (hasNewAssignee) {
            newAssignee = userRepository.findById(request.newAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_ASSIGNEE, "Không tìm thấy IT Staff được chọn"));

            if (newAssignee.getRole() == null || !"IT_STAFF".equalsIgnoreCase(newAssignee.getRole().getRoleName())
                    || Boolean.FALSE.equals(newAssignee.getIsActive())) {
                throw new AppException(ErrorCode.INVALID_ASSIGNEE, "Người được giao phải là IT Staff đang hoạt động");
            }

            // Kết thúc assignment cũ
            assignmentRepository.findByTicket_IdAndIsCurrentTrue(ticket.getId()).ifPresent(old -> {
                old.end(now);
                assignmentRepository.save(old);
            });

            // Tạo assignment mới
            TicketAssignment newAssignment = new TicketAssignment(
                    ticket,
                    newAssignee,
                    currentUser,
                    now,
                    "Điều phối kỹ thuật viên mới sau khi duyệt mở lại vé: " + (reviewNote != null ? reviewNote : "")
            );
            assignmentRepository.save(newAssignment);
            ticket.setCurrentAssignee(newAssignee);
        }

        // 5. Chuyển trạng thái ticket sang REOPENED
        transitionValidator.assertTransitionAllowed(currentStatus, TicketStatus.REOPENED);
        ticket.setStatus(TicketStatus.REOPENED.name());
        ticketRepository.save(ticket);

        // 6. Ghi TicketHistory (HD09)
        String historyNote = "Duyệt mở lại ticket (Đợt " + (currentCount + 1) + ")"
                + (reviewNote != null && !reviewNote.isBlank() ? " - Ghi chú: " + reviewNote : "")
                + (hasNewAssignee ? " - Phân công lại cho: " + newAssignee.getFullName() : " - Kỹ thuật viên cũ tiếp tục xử lý");

        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "REOPEN_APPROVE",
                "status",
                TicketStatus.REOPEN_PENDING.name(),
                TicketStatus.REOPENED.name(),
                historyNote,
                clientIp,
                now
        ));

        // 7. Security Audit Log (SDD §7)
        Long actorId = currentUser.getId();
        String ticketNumber = ticket.getTicketNumber();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditLogger.log("REOPEN_APPROVED", actorId, roleName, "TICKET", ticketNumber,
                        "REOPEN_APPROVE", "SUCCESS", 200);
            }
        });

        return toResponse(reopenRequest);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/reopen-requests/{id}/reject — IT Manager từ chối mở lại (BPMN P3)
    // -------------------------------------------------------------------------

    @Transactional
    public ReopenRequestResponse rejectReopenRequest(Long requestId, RejectReopenRequest request, String ipAddress) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null;

        ReopenRequest reopenRequest = reopenRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu mở lại ticket"));

        if (!ReopenRequest.STATUS_PENDING.equalsIgnoreCase(reopenRequest.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Yêu cầu mở lại này đã được xử lý trước đó (trạng thái: " + reopenRequest.getStatus() + ")");
        }

        String rejectionReason = (request != null && request.reviewNote() != null) ? request.reviewNote().trim() : "";
        if (rejectionReason.length() < 10) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Lý do từ chối mở lại bắt buộc và phải có ít nhất 10 ký tự");
        }

        Ticket ticket = reopenRequest.getTicket();
        TicketStatus currentStatus;
        try {
            currentStatus = TicketStatus.valueOf(ticket.getStatus());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (currentStatus != TicketStatus.REOPEN_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Ticket không ở trạng thái chờ duyệt mở lại (hiện tại: " + ticket.getStatus() + ")");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";

        // 1. Cập nhật ReopenRequest sang REJECTED
        reopenRequest.setStatus(ReopenRequest.STATUS_REJECTED);
        reopenRequest.setReviewedBy(currentUser);
        reopenRequest.setReviewNote(rejectionReason);
        reopenRequest.setReviewedAt(now);
        reopenRequestRepository.save(reopenRequest);

        // 2. Chuyển trạng thái ticket quay trở lại RESOLVED (BPMN P3: Revert Status to 'Resolved' & Record History)
        transitionValidator.assertTransitionAllowed(currentStatus, TicketStatus.RESOLVED);
        ticket.setStatus(TicketStatus.RESOLVED.name());
        ticketRepository.save(ticket);

        // 3. Ghi TicketHistory (HD09)
        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "REOPEN_REJECT",
                "status",
                TicketStatus.REOPEN_PENDING.name(),
                TicketStatus.RESOLVED.name(),
                "Từ chối yêu cầu mở lại: " + rejectionReason,
                clientIp,
                now
        ));

        // 4. Security Audit Log (SDD §7)
        Long actorId = currentUser.getId();
        String ticketNumber = ticket.getTicketNumber();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditLogger.log("REOPEN_REJECTED", actorId, roleName, "TICKET", ticketNumber,
                        "REOPEN_REJECT", "SUCCESS", 200);
            }
        });

        return toResponse(reopenRequest);
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
