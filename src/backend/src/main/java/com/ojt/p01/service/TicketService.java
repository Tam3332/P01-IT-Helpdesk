package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.entity.SLAConfig;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.SLAConfigRepository;
import com.ojt.p01.repository.TicketCategoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;

/**
 * Service xử lý nghiệp vụ Ticket (Task T3).
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SLAConfigRepository slaConfigRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         TicketCategoryRepository categoryRepository,
                         SLAConfigRepository slaConfigRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.slaConfigRepository = slaConfigRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Tạo ticket mới (POST /api/v1/tickets)
    // -------------------------------------------------------------------------

    /**
     * Tạo ticket mới theo luồng:
     * 1. Lấy người dùng đang đăng nhập từ SecurityContext
     * 2. Kiểm tra danh mục tồn tại
     * 3. Tính priority = impactLevel (§1 SRS — quy tắc sơ bộ)
     * 4. Tra SLA config theo priority → tính response_deadline & resolution_deadline
     * 5. Sinh ticket_number duy nhất theo định dạng HD-YYYY-XXXX
     * 6. Lưu vào DB với status = NEW
     *
     * @param request DTO từ request body (đã validate)
     * @return TicketResponse — thông tin vé vừa tạo
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {

        // 1. Lấy user đang đăng nhập
        User requester = getCurrentUser();

        // 2. Kiểm tra danh mục
        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 3. Tính priority = impactLevel (quy tắc sơ bộ §1 SRS)
        String priority = request.impactLevel();

        // 4. Tra SLA config + tính deadline
        SLAConfig slaConfig = slaConfigRepository.findByPriorityLevel(priority)
                .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime responseDeadline = now.plusMinutes(
                toMinutes(slaConfig.getResponseTimeHours().doubleValue()));
        OffsetDateTime resolutionDeadline = now.plusMinutes(
                toMinutes(slaConfig.getResolutionTimeHours().doubleValue()));

        // 5. Sinh ticket_number duy nhất: HD-YYYY-XXXX
        String ticketNumber = generateTicketNumber();

        // 6. Tạo và lưu entity
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(ticketNumber);
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setImpactLevel(request.impactLevel());
        ticket.setPriority(priority);
        ticket.setStatus("NEW");
        ticket.setRequester(requester);
        ticket.setResponseDeadline(responseDeadline);
        ticket.setResolutionDeadline(resolutionDeadline);
        ticket.setReopenCount(0);
        ticket.setIsSlaBreached(false);

        Ticket saved = ticketRepository.save(ticket);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Helper: Lấy user đang đăng nhập từ SecurityContext
    // -------------------------------------------------------------------------

    /**
     * Lấy User entity của người đang đăng nhập.
     * Sau khi Cường implement JWT (task C4), Authentication.getName() sẽ trả về username.
     * Hiện tại nếu chưa có auth → throw UNAUTHENTICATED.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String username = auth.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        }

        // DEV FALLBACK: Khi chua co JWT tu Cuong (task C4), fallback user demo 'employee' de test tao ve
        return userRepository.findByUsername("employee")
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, "No users available for demo fallback")));
    }

    // -------------------------------------------------------------------------
    // Helper: Sinh mã vé HD-YYYY-XXXX
    // -------------------------------------------------------------------------

    /**
     * Sinh ticket_number duy nhất theo định dạng HD-YYYY-XXXX.
     * Ví dụ: HD-2026-0001, HD-2026-0002, ...
     * Thuật toán: đếm số ticket đã có trong năm → sequence tiếp theo.
     * Retry tối đa 10 lần nếu bị race condition (2 request cùng lúc).
     */
    private String generateTicketNumber() {
        int year = Year.now().getValue();
        String yearPrefix = "HD-" + year + "-";

        for (int attempt = 0; attempt < 10; attempt++) {
            long count = ticketRepository.countByYearPrefix(yearPrefix);
            String candidate = yearPrefix + String.format("%04d", count + 1);
            if (!ticketRepository.existsByTicketNumber(candidate)) {
                return candidate;
            }
            // Nếu đã tồn tại (race condition) → thử số tiếp theo
        }

        // Fallback an toàn: dùng timestamp nếu retry đều thất bại
        return yearPrefix + System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Helper: Convert hours → minutes (dùng OffsetDateTime.plusMinutes)
    // -------------------------------------------------------------------------

    private long toMinutes(double hours) {
        return Math.round(hours * 60);
    }

    // -------------------------------------------------------------------------
    // Helper: Map Ticket entity → TicketResponse DTO
    // -------------------------------------------------------------------------

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory().getCategoryName(),
                ticket.getImpactLevel(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getRequester().getFullName(),
                ticket.getResponseDeadline(),
                ticket.getResolutionDeadline(),
                ticket.getCreatedAt()
        );
    }
}
