package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateFeedbackRequest;
import com.ojt.p01.dto.response.FeedbackResponse;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketFeedback;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketFeedbackRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class FeedbackService {

    private final TicketFeedbackRepository feedbackRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public FeedbackService(TicketFeedbackRepository feedbackRepository,
                           TicketRepository ticketRepository,
                           TicketHistoryRepository historyRepository,
                           UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy thông tin đánh giá CSAT của ticket (GET /api/v1/tickets/{id}/feedback).
     * Trả về null nếu vé chưa được đánh giá.
     */
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(String idOrNumber) {
        Ticket ticket = findTicket(idOrNumber);
        validateAccess(ticket);

        return feedbackRepository.findByTicket_Id(ticket.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Gửi đánh giá mức độ hài lòng (POST /api/v1/tickets/{id}/feedback).
     * - Chỉ người tạo vé (Employee) mới được đánh giá (Anti-IDOR).
     * - Chỉ được đánh giá khi vé ở trạng thái CLOSED.
     * - Mỗi ticket chỉ được đánh giá 1 lần duy nhất.
     */
    @Transactional
    public FeedbackResponse createFeedback(String idOrNumber, CreateFeedbackRequest request, String ipAddress) {
        Ticket ticket = findTicket(idOrNumber);
        User currentUser = getCurrentUser();

        // 1. Kiểm tra quyền: Chỉ Employee tạo vé mới được đánh giá
        if (!ticket.getRequester().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ người tạo ticket mới có quyền gửi đánh giá");
        }

        // 2. Kiểm tra trạng thái: Ticket phải ở trạng thái CLOSED
        if (!"CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ có thể đánh giá sau khi ticket đã hoàn tất đóng");
        }

        // 3. Kiểm tra trùng lặp: Đã đánh giá chưa
        if (feedbackRepository.existsByTicket_Id(ticket.getId())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ticket này đã được gửi đánh giá trước đó");
        }

        // 4. Lưu đánh giá
        TicketFeedback feedback = new TicketFeedback(
                ticket,
                currentUser,
                request.ratingStars(),
                request.feedbackComment() != null ? request.feedbackComment().trim() : null
        );
        TicketFeedback saved = feedbackRepository.save(feedback);

        // 5. Ghi nhận vào nhật ký TicketHistory (HD09)
        OffsetDateTime now = OffsetDateTime.now();
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        String note = "Đánh giá chất lượng hỗ trợ: " + request.ratingStars() + " sao" +
                      (request.feedbackComment() != null && !request.feedbackComment().isBlank()
                              ? " - Ý kiến: \"" + request.feedbackComment().trim() + "\""
                              : "");

        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "FEEDBACK",
                "feedback",
                null,
                request.ratingStars() + " sao",
                note,
                clientIp,
                now
        ));

        return toResponse(saved);
    }

    private FeedbackResponse toResponse(TicketFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getTicket().getId(),
                feedback.getRequester().getId(),
                feedback.getRequester().getFullName(),
                feedback.getRatingStars(),
                feedback.getFeedbackComment(),
                feedback.getCreatedAt()
        );
    }

    private Ticket findTicket(String idOrNumber) {
        try {
            Long id = Long.parseLong(idOrNumber);
            return ticketRepository.findById(id)
                    .orElseGet(() -> ticketRepository.findByTicketNumber(idOrNumber)
                            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND)));
        } catch (NumberFormatException e) {
            return ticketRepository.findByTicketNumber(idOrNumber)
                    .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));
        }
    }

    private void validateAccess(Ticket ticket) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            if (!ticket.getRequester().getId().equals(currentUser.getId())) {
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            if (!isAssignedToMe) {
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        }
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
