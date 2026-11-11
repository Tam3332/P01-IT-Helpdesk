package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateCommentRequest;
import com.ojt.p01.dto.response.CommentResponse;
import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketComment;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketCommentRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service xử lý bình luận và trao đổi trực tiếp trên ticket.
 */
@Service
public class CommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public CommentService(TicketCommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          TicketHistoryRepository historyRepository,
                          UserRepository userRepository,
                          AuditLogger auditLogger) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

 /**
 * Lấy danh sách bình luận của ticket (GET /api/v1/tickets/{id}/comments).
 * Phân quyền:
 * Employee chỉ thấy bình luận công khai (is_internal = false).
 * IT Staff, IT Manager, Admin thấy tất cả (cả ghi chú nội bộ).
 */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String idOrNumber) {
        Ticket ticket = findTicket(idOrNumber);
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // Anti-IDOR: Employee chỉ xem vé của mình, Staff chỉ xem vé được giao (trái quyền → 404 + audit AUTHZ_DENIED)
        if ("EMPLOYEE".equalsIgnoreCase(roleName) && !ticket.getRequester().getId().equals(currentUser.getId())) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "VIEW_COMMENTS", 404);
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }
        if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            if (!isAssignedToMe) {
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "VIEW_COMMENTS", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        }

        List<TicketComment> comments;
        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            comments = commentRepository.findByTicket_IdAndIsInternalFalseOrderByCreatedAtAsc(ticket.getId());
        } else {
            comments = commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        }

        return comments.stream().map(this::toResponse).toList();
    }

 /**
 * Gửi bình luận mới hoặc ghi chú nội bộ (POST /api/v1/tickets/{id}/comments).
 * Ticket CLOSED không nhận bình luận (422).
 * Employee gửi isInternal = true sẽ tự động ép về false.
 */
    @Transactional
    public CommentResponse addComment(String idOrNumber, CreateCommentRequest request, String ipAddress) {
        Ticket ticket = findTicket(idOrNumber);

        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // Anti-IDOR: Employee chỉ bình luận trên vé của mình (trái quyền → 404 + audit AUTHZ_DENIED)
        if ("EMPLOYEE".equalsIgnoreCase(roleName) && !ticket.getRequester().getId().equals(currentUser.getId())) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "ADD_COMMENT", 404);
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

        // IT Staff chỉ bình luận trên vé được giao cho mình.
        // Vé không được giao thì Staff cũng không xem được → 404 như GET (Anti-IDOR)
        if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            if (!isAssignedToMe) {
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "ADD_COMMENT", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        }

        // Admin chỉ đọc ticket, không bình luận
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "ADD_COMMENT", 403);
            throw new AppException(ErrorCode.FORBIDDEN, "Quản trị viên chỉ xem ticket, không được bình luận");
        }

        // Kiểm tra trạng thái SAU khi kiểm tra quyền: người không có quyền luôn nhận 404/403
        // không biết được ticket có tồn tại hay đã đóng (Anti-IDOR)
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new AppException(ErrorCode.TICKET_CLOSED);
        }

        // Quy tắc is_internal: chỉ IT Staff, IT Manager mới được ghi chú nội bộ
        boolean isInternal = Boolean.TRUE.equals(request.isInternal());
        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            isInternal = false;
        }

        TicketComment comment = new TicketComment(ticket, currentUser, request.commentText().trim(), isInternal);
        TicketComment saved = commentRepository.save(comment);

        // Ghi nhận vào TicketHistory
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        String note = isInternal ? "[Ghi chú nội bộ]" : "[Bình luận công khai]";
        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "COMMENT",
                "comment_id",
                null,
                String.valueOf(saved.getId()),
                note,
                clientIp,
                saved.getCreatedAt()
        ));

        return toResponse(saved);
    }

    private CommentResponse toResponse(TicketComment c) {
        String roleName = c.getAuthor().getRole() != null ? c.getAuthor().getRole().getRoleName() : "";
        return new CommentResponse(
                c.getId(),
                c.getTicket().getId(),
                c.getAuthor().getId(),
                c.getAuthor().getFullName(),
                roleName,
                c.getCommentText(),
                c.getIsInternal(),
                c.getCreatedAt()
        );
    }

    private Ticket findTicket(String idOrNumber) {
        try {
            Long id = Long.parseLong(idOrNumber.trim());
            return ticketRepository.findById(id)
                    .orElseGet(() -> ticketRepository.findByTicketNumber(idOrNumber.trim())
                            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND)));
        } catch (NumberFormatException e) {
            return ticketRepository.findByTicketNumber(idOrNumber.trim())
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
