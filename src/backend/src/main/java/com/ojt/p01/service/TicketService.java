package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.TicketDetailResponse;
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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý nghiệp vụ Ticket (Task T3 Tạo vé, Task T4 Danh sách & Chi tiết vé).
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
    // Task T3: Tạo ticket mới (POST /api/v1/tickets)
    // -------------------------------------------------------------------------

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
    // Task T4: Lấy danh sách ticket có phân trang & lọc theo Role (GET /api/v1/tickets)
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách ticket có phân quyền và bộ lọc:
     * - EMPLOYEE: chỉ xem được vé do chính mình tạo (requester_id = currentUser.id).
     * - IT_STAFF, IT_MANAGER, ADMIN: xem được tất cả vé trong hệ thống.
     *
     * @param page     Số trang (0-indexed)
     * @param size     Kích thước trang
     * @param status   Lọc trạng thái (tùy chọn: NEW, ASSIGNED, ...)
     * @param priority Lọc độ ưu tiên (tùy chọn: LOW, MEDIUM, ...)
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTickets(int page, int size, String status, String priority) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Phân quyền theo Role: EMPLOYEE chỉ xem vé của chính mình
            if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
                predicates.add(cb.equal(root.get("requester").get("id"), currentUser.getId()));
            }

            // 2. Bộ lọc trạng thái (nếu có)
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }

            // 3. Bộ lọc mức độ ưu tiên (nếu có)
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(root.get("priority"), priority.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TicketResponse> pageResult = ticketRepository.findAll(spec, pageRequest)
                .map(this::toResponse);

        return PageResponse.from(pageResult);
    }

    // -------------------------------------------------------------------------
    // Task T4: Xem chi tiết 1 ticket (GET /api/v1/tickets/{ticketNumber})
    // -------------------------------------------------------------------------

    /**
     * Xem thông tin chi tiết một vé cụ thể.
     * Ràng buộc bảo mật (chống IDOR - IA-HD01, IA-HD02):
     * - EMPLOYEE chỉ được mở vé do chính mình tạo ra. Nếu truy cập vé người khác -> 403 FORBIDDEN.
     * - IT_STAFF, IT_MANAGER, ADMIN được phép xem mọi vé.
     *
     * @param ticketNumber Mã vé (ví dụ: HD-2026-0001)
     */
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // Kiểm tra quyền hạn: EMPLOYEE chỉ được xem vé của mình
        if ("EMPLOYEE".equalsIgnoreCase(roleName) && !ticket.getRequester().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập thông tin vé này");
        }

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Helper: Lấy user đang đăng nhập từ SecurityContext
    // -------------------------------------------------------------------------

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String username = auth.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        }

        // DEV FALLBACK: Khi chưa có JWT từ Cường (task C4), fallback user demo 'employee'
        return userRepository.findByUsername("employee")
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, "No users available for demo fallback")));
    }

    // -------------------------------------------------------------------------
    // Helper: Sinh mã vé HD-YYYY-XXXX
    // -------------------------------------------------------------------------

    private String generateTicketNumber() {
        int year = Year.now().getValue();
        String yearPrefix = "HD-" + year + "-";

        for (int attempt = 0; attempt < 10; attempt++) {
            long count = ticketRepository.countByYearPrefix(yearPrefix);
            String candidate = yearPrefix + String.format("%04d", count + 1);
            if (!ticketRepository.existsByTicketNumber(candidate)) {
                return candidate;
            }
        }

        return yearPrefix + System.currentTimeMillis();
    }

    private long toMinutes(double hours) {
        return Math.round(hours * 60);
    }

    // -------------------------------------------------------------------------
    // Helper: Map Ticket entity -> TicketResponse / TicketDetailResponse DTO
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

    private TicketDetailResponse toDetailResponse(Ticket ticket) {
        User assignee = ticket.getCurrentAssignee();
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory().getId(),
                ticket.getCategory().getCategoryCode(),
                ticket.getCategory().getCategoryName(),
                ticket.getImpactLevel(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getRequester().getId(),
                ticket.getRequester().getFullName(),
                ticket.getRequester().getEmail(),
                ticket.getRequester().getDepartment(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getFullName() : null,
                assignee != null ? assignee.getEmail() : null,
                ticket.getResponseDeadline(),
                ticket.getResolutionDeadline(),
                ticket.getIsSlaBreached(),
                ticket.getFirstRespondedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getReopenedAt(),
                ticket.getReopenCount(),
                ticket.getReopenReason(),
                ticket.getResolutionNote(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
