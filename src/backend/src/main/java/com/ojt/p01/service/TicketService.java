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
 * Service xử lý nghiệp vụ Ticket (HD03 Tạo vé, HD05 Danh sách & Chi tiết vé).
 * Tuân thủ Access Matrix (02 §5), UC-02, UC-04, Anti-IDOR (IA-HD02), T3-09.
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
    // HD03: Tạo ticket mới (POST /api/v1/tickets)
    // -------------------------------------------------------------------------

    /**
     * Tạo ticket mới.
     * Quy tắc phân quyền: CHỈ EMPLOYEE được tạo ticket (Access Matrix §5, UC-02).
     * Role khác gọi đến → 403 FORBIDDEN.
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {

        // 1. Lấy user đang đăng nhập
        User requester = getCurrentUser();
        String roleName = requester.getRole() != null ? requester.getRole().getRoleName() : "";

        // Kiểm tra role: Chỉ EMPLOYEE được tạo ticket
        if (!"EMPLOYEE".equalsIgnoreCase(roleName)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ nhân viên (EMPLOYEE) mới có quyền tạo phiếu yêu cầu");
        }

        // 2. Kiểm tra danh mục tồn tại
        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 3. UC-02 5a: Chặn danh mục đã bị vô hiệu hóa (isActive = false)
        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 4. Tính priority = impactLevel (quy tắc sơ bộ ở SRS)
        String priority = request.impactLevel();

        // 5. Tra SLA config + tính deadline (BR-HD-03)
        SLAConfig slaConfig = slaConfigRepository.findByPriorityLevel(priority)
                .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime responseDeadline = now.plusMinutes(
                toMinutes(slaConfig.getResponseTimeHours().doubleValue()));
        OffsetDateTime resolutionDeadline = now.plusMinutes(
                toMinutes(slaConfig.getResolutionTimeHours().doubleValue()));

        // 6. Sinh ticket_number duy nhất: HD-YYYY-XXXX
        String ticketNumber = generateTicketNumber();

        // 7. Tạo và lưu entity
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
    // HD05: Lấy danh sách ticket có phân trang & lọc theo Role (GET /api/v1/tickets)
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách ticket có phân quyền và bộ lọc:
     * - EMPLOYEE   : chỉ xem được vé do chính mình tạo (requester_id = currentUser.id).
     * - IT_STAFF   : xem vé được giao cho mình (currentAssignee) + tất cả vé trạng thái NEW (chờ tiếp nhận).
     * - IT_MANAGER : xem toàn bộ vé trong hệ thống.
     * - ADMIN      : xem toàn bộ vé (chế độ chỉ đọc).
     *
     * @param page     Số trang (0-indexed)
     * @param size     Kích thước trang (mặc định 20 theo OpenAPI)
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

            // 1. Phân quyền Data-Scoping theo Role
            if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
                // EMPLOYEE chỉ thấy vé của chính mình
                predicates.add(cb.equal(root.get("requester").get("id"), currentUser.getId()));

            } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
                // IT_STAFF thấy: vé được giao cho mình HOẶC vé NEW trong hàng đợi
                Predicate assignedToMe = cb.equal(root.get("currentAssignee").get("id"), currentUser.getId());
                Predicate isNew = cb.equal(root.get("status"), "NEW");
                predicates.add(cb.or(assignedToMe, isNew));
            }
            // IT_MANAGER, ADMIN: thấy tất cả vé

            // 2. Bộ lọc trạng thái (nếu có)
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }

            // 3. Bộ lọc độ ưu tiên (nếu có)
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
    // HD05: Xem chi tiết 1 ticket (GET /api/v1/tickets/{id})
    // -------------------------------------------------------------------------

    /**
     * Xem thông tin chi tiết một vé cụ thể theo ID số (Long) hoặc mã chuỗi (HD-YYYY-XXXX).
     *
     * Kiểm tra Owner ở Service Layer theo chính sách T3-09 & Anti-IDOR (IA-HD02):
     * - EMPLOYEE: chỉ được xem vé do chính mình tạo ra. Xem vé người khác → 404 NOT_FOUND.
     * - IT_STAFF: chỉ được xem vé được giao cho mình hoặc vé đang ở trạng thái NEW. Xem vé khác → 404 NOT_FOUND.
     * - IT_MANAGER, ADMIN: xem được tất cả vé.
     *
     * Lý do trả 404 thay vì 403: Bảo vệ che giấu sự tồn tại của tài nguyên, chống tấn công IDOR thu thập ID.
     *
     * @param idOrNumber ID số của vé (ví dụ: 1) HOẶC mã vé (ví dụ: HD-2026-0001)
     */
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(String idOrNumber) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);

        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // Kiểm tra quyền sở hữu / phạm vi truy cập:
        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            // EMPLOYEE cố truy cập vé người khác → 404 Anti-IDOR
            if (!ticket.getRequester().getId().equals(currentUser.getId())) {
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            // IT_STAFF chỉ xem vé được giao cho mình HOẶC vé NEW
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            boolean isNewTicket = "NEW".equalsIgnoreCase(ticket.getStatus());

            if (!isAssignedToMe && !isNewTicket) {
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
        }
        // IT_MANAGER và ADMIN được xem mọi vé

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Helper: Tìm vé bằng ID số hoặc Mã vé chuỗi
    // -------------------------------------------------------------------------

    private Ticket findTicketByIdOrNumber(String idOrNumber) {
        if (idOrNumber == null || idOrNumber.isBlank()) {
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

        // Nếu là số nguyên: tìm theo ID trước
        try {
            Long id = Long.parseLong(idOrNumber.trim());
            return ticketRepository.findById(id)
                    .orElseGet(() -> ticketRepository.findByTicketNumber(idOrNumber.trim())
                            .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND)));
        } catch (NumberFormatException ignored) {
            // Không phải số: tìm theo mã HD-YYYY-XXXX
            return ticketRepository.findByTicketNumber(idOrNumber.trim())
                    .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));
        }
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

        // Không có JWT hợp lệ → 401
        throw new AppException(ErrorCode.UNAUTHENTICATED);
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
