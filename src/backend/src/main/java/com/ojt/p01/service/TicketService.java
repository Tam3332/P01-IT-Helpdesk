package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.AssignTicketRequest;
import com.ojt.p01.dto.request.UpdateTicketStatusRequest;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.dto.response.TicketHistoryResponse;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.entity.SLAConfig;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketAssignment;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.SLAConfigRepository;
import com.ojt.p01.repository.TicketAssignmentRepository;
import com.ojt.p01.repository.TicketCategoryRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ Ticket.
 * Tuân thủ Access Matrix (02 §5), , Anti-IDOR.
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SLAConfigRepository slaConfigRepository;
    private final UserRepository userRepository;
    private final TicketAssignmentRepository assignmentRepository;
    private final TicketHistoryRepository historyRepository;
    private final TicketStatusTransitionValidator transitionValidator;
    private final AuditLogger auditLogger;

    public TicketService(TicketRepository ticketRepository,
                         TicketCategoryRepository categoryRepository,
                         SLAConfigRepository slaConfigRepository,
                         UserRepository userRepository,
                         TicketAssignmentRepository assignmentRepository,
                         TicketHistoryRepository historyRepository,
                         TicketStatusTransitionValidator transitionValidator,
                         AuditLogger auditLogger) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.slaConfigRepository = slaConfigRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.transitionValidator = transitionValidator;
        this.auditLogger = auditLogger;
    }

    // -------------------------------------------------------------------------
    // Tạo ticket mới (POST /api/v1/tickets)
    // -------------------------------------------------------------------------

 /**
 * Tạo ticket mới.
 * Quy tắc phân quyền: CHỈ EMPLOYEE được tạo ticket (Access Matrix §5).
 * Role khác gọi đến → 403 FORBIDDEN.
 */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        return createTicket(request, "127.0.0.1");
    }

 /**
 * Tạo ticket mới và ghi nhận TicketHistory ban đầu (action=CREATE).
 */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, String ipAddress) {

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

        // 3. : Chặn danh mục đã bị vô hiệu hóa (isActive = false)
        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 4. Tính priority = impactLevel (quy tắc sơ bộ ở)
        String priority = request.impactLevel();

        // 5. Tra SLA config + tính deadline
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

        // Ghi nhận lịch sử tạo ticket
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        historyRepository.save(new TicketHistory(
                saved,
                requester,
                "CREATE",
                "status",
                null,
                "NEW",
                "Tạo mới ticket",
                clientIp,
                saved.getCreatedAt()
        ));

        // Audit TICKET_CREATED: chỉ ghi sau khi transaction commit thành công
        // tránh có dòng log cho ticket thực tế không được lưu
        Long actorId = requester.getId();
        String actorRole = requester.getRole() != null ? requester.getRole().getRoleName() : null;
        String createdNumber = saved.getTicketNumber();
        auditLogger.logAfterCommit("TICKET_CREATED", actorId, actorRole, "TICKET", createdNumber, "CREATE", "SUCCESS", 201, null, null);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Lấy danh sách ticket có phân trang & lọc theo Role (GET /api/v1/tickets)
    // -------------------------------------------------------------------------

 /**
 * Lấy danh sách ticket có phân quyền và bộ lọc:
 * EMPLOYEE : chỉ xem được vé do chính mình tạo (requester_id = currentUser.id).
 * IT_STAFF : xem vé được giao cho mình (currentAssignee) + tất cả vé trạng thái NEW (chờ tiếp nhận).
 * IT_MANAGER : xem toàn bộ vé trong hệ thống.
 * ADMIN : xem toàn bộ vé (chế độ chỉ đọc).
 *
 * @param page Số trang (0-indexed)
 * @param size Kích thước trang (mặc định 20 theo OpenAPI)
 * @param status Lọc trạng thái (tùy chọn: NEW, ASSIGNED...)
 * @param priority Lọc độ ưu tiên (tùy chọn: LOW, MEDIUM...)
 */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTickets(int page, int size, String status, String priority,
                                                   Integer categoryId, Long assigneeId, String search) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        // tham số trang không hợp lệ thì dùng giá trị mặc định (trang 0, 20 dòng –)
        int safePage = page < 0 ? 0 : page;
        int safeSize = size < 1 ? 20 : size;
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Phân quyền Data-Scoping theo Role
            if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
                // EMPLOYEE chỉ thấy vé của chính mình
                predicates.add(cb.equal(root.get("requester").get("id"), currentUser.getId()));

            } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
                // IT_STAFF chỉ thấy vé được giao cho mình
                predicates.add(cb.equal(root.get("currentAssignee").get("id"), currentUser.getId()));
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

            // 4–6. Bộ lọc (categoryId, assigneeId, search). Tất cả AND thêm SAU phạm vi theo role ở bước 1
            // nên không thể dùng bộ lọc để lấy ticket ngoài phạm vi.
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("currentAssignee").get("id"), assigneeId));
            }
            if (search != null && !search.isBlank()) {
                // Tìm theo tiêu đề hoặc mô tả, không phân biệt hoa thường. Thoát %, _ để người dùng gõ ký tự này không thành wildcard.
                String kw = "%" + search.trim().toLowerCase()
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), kw, '\\'),
                        cb.like(cb.lower(root.get("description")), kw, '\\')));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TicketResponse> pageResult = ticketRepository.findAll(spec, pageRequest)
                .map(this::toResponse);

        return PageResponse.from(pageResult);
    }

    // -------------------------------------------------------------------------
    // Xem chi tiết 1 ticket (GET /api/v1/tickets/{id})
    // -------------------------------------------------------------------------

 /**
 * Xem thông tin chi tiết một vé cụ thể theo ID số (Long) hoặc mã chuỗi (HD-YYYY-XXXX).
 *
 * Kiểm tra Owner ở Service Layer theo chính sách & Anti-IDOR:
 * EMPLOYEE: chỉ được xem vé do chính mình tạo ra. Xem vé người khác → 404 NOT_FOUND.
 * IT_STAFF: chỉ được xem vé được giao cho mình hoặc vé đang ở trạng thái NEW. Xem vé khác → 404 NOT_FOUND.
 * IT_MANAGER, ADMIN: xem được tất cả vé.
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
                throw denyTicketAccess(currentUser, roleName, ticket, "VIEW_TICKET");
            }
        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            // IT_STAFF chỉ xem vé được giao cho mình; vé khác → 404 + audit AUTHZ_DENIED
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());
            if (!isAssignedToMe) {
                throw denyTicketAccess(currentUser, roleName, ticket, "VIEW_TICKET");
            }
        }
        // IT_MANAGER và ADMIN được xem mọi vé

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Phân công / điều phối lại (PUT /api/v1/tickets/{id}/assign) —
    // -------------------------------------------------------------------------

 /** Trạng thái được phép phân công: tiền điều kiện + Reopened. */
    private static final EnumSet<TicketStatus> ASSIGNABLE_STATUSES =
            EnumSet.of(TicketStatus.NEW, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.REOPENED);

 /**
 * Phân công lần đầu (ticket NEW) hoặc điều phối lại (ASSIGNED / IN_PROGRESS / REOPENED).
 * Toàn bộ chạy trong 1 transaction: lỗi ở bất kỳ bước nào thì rollback hết.
 *
 * Lần đầu: NEW → ASSIGNED, chốt priority, tính lại 2 hạn SLA = created_at + giờ theo priority.
 * Điều phối lại ticket ASSIGNED / IN_PROGRESS: giữ nguyên trạng thái, không tính lại SLA
 *.
 * Phân công lại sau Reopen: REOPENED → ASSIGNED, IT Staff nhận thì sang IN_PROGRESS;
 * không tính lại SLA vì hạn xử lý mới đã tính lúc duyệt mở lại.
 * Không ghi first_responded_at: P2 "Hạn phản hồi dừng tính ngay khi IT Staff tiếp nhận (In Progress)".
 */
    @Transactional
    public TicketDetailResponse assignTicket(String idOrNumber, AssignTicketRequest request, String ipAddress) {
        User manager = getCurrentUser();

        // Lớp 2 kiểm quyền (lớp 1 là SecurityConfig): chỉ IT_MANAGER
        String roleName = manager.getRole() != null ? manager.getRole().getRoleName() : "";
        if (!"IT_MANAGER".equalsIgnoreCase(roleName)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        Ticket ticket = findTicketByIdOrNumber(idOrNumber); // không có → 404

        TicketStatus current = TicketStatus.valueOf(ticket.getStatus());
        if (!ASSIGNABLE_STATUSES.contains(current)) {
            throw new AppException(ErrorCode.TICKET_NOT_ASSIGNABLE,
                    "Không thể phân công ticket đang ở trạng thái " + current.name());
        }

        // Người được giao: phải tồn tại, role IT_STAFF, đang hoạt động
        User assignee = userRepository.findById(request.assigneeId()).orElse(null);
        boolean validAssignee = assignee != null
                && assignee.getRole() != null
                && "IT_STAFF".equalsIgnoreCase(assignee.getRole().getRoleName())
                && Boolean.TRUE.equals(assignee.getIsActive());
        if (!validAssignee) {
            throw new AppException(ErrorCode.INVALID_ASSIGNEE);
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean firstAssign = current == TicketStatus.NEW;
        boolean afterReopen = current == TicketStatus.REOPENED;
        User oldAssignee = ticket.getCurrentAssignee();
        String oldPriority = ticket.getPriority();
        String newPriority = request.priority() != null ? request.priority() : oldPriority;
        String note = request.assignmentNote() != null && !request.assignmentNote().isBlank()
                ? request.assignmentNote().trim() : null;

        // ticket ASSIGNED / IN_PROGRESS mà giao lại đúng người đang phụ trách
        // và không đổi priority → 422, không ghi lịch sử. REOPENED thì vẫn cho giao lại người cũ (chuyển sang ASSIGNED).
        boolean sameAssignee = oldAssignee != null && oldAssignee.getId().equals(assignee.getId());
        boolean reassignable = current == TicketStatus.ASSIGNED || current == TicketStatus.IN_PROGRESS;
        if (reassignable && sameAssignee && newPriority.equals(oldPriority)) {
            throw new AppException(ErrorCode.ASSIGNEE_UNCHANGED);
        }

        // / : kết thúc phân công cũ TRƯỚC (flush ngay), rồi mới thêm dòng mới
        // để không vi phạm unique index "mỗi ticket 1 dòng is_current = true"
        assignmentRepository.findByTicket_IdAndIsCurrentTrue(ticket.getId()).ifPresent(old -> {
            old.end(now);
            assignmentRepository.saveAndFlush(old);
        });
        assignmentRepository.save(new TicketAssignment(ticket, assignee, manager, now, note));

        ticket.setCurrentAssignee(assignee);
        ticket.setPriority(newPriority);

        if (firstAssign) {
            transitionValidator.assertTransitionAllowed(TicketStatus.NEW, TicketStatus.ASSIGNED);
            ticket.setStatus(TicketStatus.ASSIGNED.name());

            // hạn = created_at + giờ theo SLAConfig của mức ưu tiên chính thức
            SLAConfig sla = slaConfigRepository.findByPriorityLevel(newPriority)
                    .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));
            OffsetDateTime created = ticket.getCreatedAt();
            ticket.setResponseDeadline(created.plusMinutes(toMinutes(sla.getResponseTimeHours().doubleValue())));
            ticket.setResolutionDeadline(created.plusMinutes(toMinutes(sla.getResolutionTimeHours().doubleValue())));
        }
        if (afterReopen) {
            transitionValidator.assertTransitionAllowed(TicketStatus.REOPENED, TicketStatus.ASSIGNED);
            ticket.setStatus(TicketStatus.ASSIGNED.name());
        }
        ticketRepository.save(ticket);

        // TicketHistory: 1 dòng người phụ trách, thêm 1 dòng nếu đổi priority
        String action = firstAssign ? "ASSIGN" : "REASSIGN";
        historyRepository.save(new TicketHistory(ticket, manager, action, "current_assignee_id",
                oldAssignee != null ? String.valueOf(oldAssignee.getId()) : null,
                String.valueOf(assignee.getId()), note, ipAddress, now));
        if (!newPriority.equals(oldPriority)) {
            historyRepository.save(new TicketHistory(ticket, manager, action, "priority",
                    oldPriority, newPriority, null, ipAddress, now));
        }

        // Audit log: TICKET_ASSIGNED kèm before/after của assignee_id
        Long managerId = manager.getId();
        String ticketNumber = ticket.getTicketNumber();
        Long oldAssigneeId = oldAssignee != null ? oldAssignee.getId() : null;
        Long newAssigneeId = assignee.getId();

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("assignee_id", oldAssigneeId);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("assignee_id", newAssigneeId);

        auditLogger.logAfterCommit("TICKET_ASSIGNED", managerId, roleName, "TICKET", ticketNumber,
                action, "SUCCESS", 200, before, after);

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Helper: Tìm vé bằng ID số hoặc Mã vé chuỗi
    // -------------------------------------------------------------------------
    // Cập nhật trạng thái xử lý ticket (PATCH /api/v1/tickets/{id}/status)
    // -------------------------------------------------------------------------

 /**
 * IT Staff nhận xử lý (IN_PROGRESS) hoặc hoàn tất sự cố (RESOLVED).
 * Chỉ IT_STAFF được giao hoặc IT_MANAGER mới được đổi trạng thái.
 * Chuyển RESOLVED bắt buộc có resolutionNote >= 20 ký tự.
 * Chuyển IN_PROGRESS: ghi nhận first_responded_at để dừng tính Response SLA.
 */
    @Transactional
    public TicketDetailResponse updateTicketStatus(String idOrNumber, UpdateTicketStatusRequest request, String ipAddress) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        // Kiểm tra quyền (Anti-IDOR & RBAC):
        boolean isManager = "IT_MANAGER".equalsIgnoreCase(roleName);
        boolean isStaff = "IT_STAFF".equalsIgnoreCase(roleName);
        if (!isManager && !isStaff) {
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "UPDATE_STATUS", 403);
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ IT Staff hoặc IT Manager mới được cập nhật trạng thái");
        }

        // Nếu là IT Staff: phải là người được phân công xử lý ticket này
        if (isStaff && !isManager) {
            if (ticket.getCurrentAssignee() == null || !ticket.getCurrentAssignee().getId().equals(currentUser.getId())) {
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "UPDATE_STATUS", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND); // Anti-IDOR
            }
        }

        TicketStatus currentStatus;
        TicketStatus targetStatus;
        try {
            currentStatus = TicketStatus.valueOf(ticket.getStatus());
            targetStatus = TicketStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION, "Trạng thái đích không hợp lệ: " + request.status());
        }

        // Kiểm tra State Machine
        transitionValidator.assertTransitionAllowed(currentStatus, targetStatus);

        // API này chỉ xử lý IN_PROGRESS và RESOLVED; trạng thái khác (vd REOPENED → ASSIGNED, đóng ticket)
        // đi qua API riêng → 422, trạng thái không đổi
        if (targetStatus != TicketStatus.IN_PROGRESS && targetStatus != TicketStatus.RESOLVED) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "API này chỉ cập nhật sang IN_PROGRESS hoặc RESOLVED");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";

        if (targetStatus == TicketStatus.IN_PROGRESS) {
            // Dừng tính Response SLA nếu chưa có mốc phản hồi đầu tiên
            if (ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(now);
            }
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);

            historyRepository.save(new TicketHistory(
                    ticket,
                    currentUser,
                    "STATUS_CHANGE",
                    "status",
                    currentStatus.name(),
                    "IN_PROGRESS",
                    "Bắt đầu tiếp nhận xử lý ticket",
                    clientIp,
                    now
            ));

        } else if (targetStatus == TicketStatus.RESOLVED) {
            // Kiểm tra resolution_note >= 20 ký tự
            String note = request.resolutionNote() != null ? request.resolutionNote().trim() : "";
            if (note.length() < 20) {
                throw new AppException(ErrorCode.RESOLUTION_NOTE_REQUIRED);
            }

            ticket.setResolvedAt(now);
            ticket.setResolutionNote(note);
            ticket.setStatus("RESOLVED");
            if (ticket.getResolutionDeadline() != null && now.isAfter(ticket.getResolutionDeadline())) {
                ticket.setIsSlaBreached(true);
            }
            ticketRepository.save(ticket);

            historyRepository.save(new TicketHistory(
                    ticket,
                    currentUser,
                    "STATUS_CHANGE",
                    "status",
                    currentStatus.name(),
                    "RESOLVED",
                    note,
                    clientIp,
                    now
            ));
        }

        // Audit log: TICKET_STATUS_CHANGED kèm before/after của status
        Long actorId = currentUser.getId();
        String ticketNumber = ticket.getTicketNumber();
        Map<String, Object> before = Map.of("status", currentStatus.name());
        Map<String, Object> after = Map.of("status", targetStatus.name());

        auditLogger.logAfterCommit("TICKET_STATUS_CHANGED", actorId, roleName, "TICKET", ticketNumber,
                "STATUS_CHANGE", "SUCCESS", 200, before, after);

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Đóng ticket hoàn tất (POST /api/v1/tickets/{id}/close)
    // -------------------------------------------------------------------------

 /**
 * Xác nhận đóng ticket sau khi được giải quyết.
 * Chỉ Employee chủ ticket hoặc IT_MANAGER mới được đóng.
 * Chỉ đóng được ticket khi đang ở trạng thái RESOLVED.
 */
    @Transactional
    public TicketDetailResponse closeTicket(String idOrNumber, String ipAddress) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        boolean isManager = "IT_MANAGER".equalsIgnoreCase(roleName);
        boolean isRequester = ticket.getRequester().getId().equals(currentUser.getId());

        if (!isManager && !isRequester) {
            if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
                auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "CLOSE_TICKET", 404);
                throw new AppException(ErrorCode.TICKET_NOT_FOUND); // Anti-IDOR
            }
            auditLogger.ticketDenied(currentUser, ticket.getTicketNumber(), "CLOSE_TICKET", 403);
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ người tạo ticket hoặc IT Manager mới có quyền đóng ticket");
        }

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (currentStatus != TicketStatus.RESOLVED) {
            throw new AppException(ErrorCode.TICKET_NOT_RESOLVED);
        }

        transitionValidator.assertTransitionAllowed(currentStatus, TicketStatus.CLOSED);

        OffsetDateTime now = OffsetDateTime.now();
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(now);
        // SLA xử lý chỉ tính tới lúc Resolved
        // không tính thời gian chờ Employee xác nhận đóng
        if (ticket.getResolvedAt() != null && ticket.getResolutionDeadline() != null
                && ticket.getResolvedAt().isAfter(ticket.getResolutionDeadline())) {
            ticket.setIsSlaBreached(true);
        }
        ticketRepository.save(ticket);

        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
        historyRepository.save(new TicketHistory(
                ticket,
                currentUser,
                "CLOSE",
                "status",
                "RESOLVED",
                "CLOSED",
                "Xác nhận đóng ticket thành công",
                clientIp,
                now
        ));

        // Audit log: TICKET_CLOSED kèm trạng thái trước khi đóng và CLOSED
        Long actorId = currentUser.getId();
        String ticketNumber = ticket.getTicketNumber();
        Map<String, Object> before = Map.of("status", currentStatus.name());
        Map<String, Object> after = Map.of("status", "CLOSED");

        auditLogger.logAfterCommit("TICKET_CLOSED", actorId, roleName, "TICKET", ticketNumber,
                "CLOSE", "SUCCESS", 200, before, after);

        return toDetailResponse(ticket);
    }

    // -------------------------------------------------------------------------
    // Xem lịch sử xử lý ticket (GET /api/v1/tickets/{id}/history)
    // -------------------------------------------------------------------------

 /**
 * Lấy toàn bộ dòng thời gian / lịch sử biến động của ticket.
 * Áp dụng kiểm tra quyền truy cập chống IDOR tương tự chi tiết ticket.
 */
    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getTicketHistory(String idOrNumber) {
        Ticket ticket = findTicketByIdOrNumber(idOrNumber);

        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "EMPLOYEE";

        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            if (!ticket.getRequester().getId().equals(currentUser.getId())) {
                throw denyTicketAccess(currentUser, roleName, ticket, "VIEW_HISTORY");
            }
        } else if ("IT_STAFF".equalsIgnoreCase(roleName)) {
            boolean isAssignedToMe = ticket.getCurrentAssignee() != null
                    && ticket.getCurrentAssignee().getId().equals(currentUser.getId());

            // IT_STAFF chỉ xem lịch sử vé được giao cho mình
            if (!isAssignedToMe) {
                throw denyTicketAccess(currentUser, roleName, ticket, "VIEW_HISTORY");
            }
        }

        List<TicketHistory> histories = historyRepository.findByTicket_IdOrderByTimestampAsc(ticket.getId());

        // Dòng phân công lưu id người xử lý (current_assignee_id). Tra tên 1 lần cho mọi id để giao diện
        // hiện đúng tên cả người xử lý cũ, không chỉ người đang phụ trách
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        for (TicketHistory h : histories) {
            if ("current_assignee_id".equals(h.getFieldChanged())) {
                addUserId(userIds, h.getOldValue());
                addUserId(userIds, h.getNewValue());
            }
        }
        java.util.Map<Long, String> userNames = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNames.put(u.getId(), u.getFullName()));
        }

        return histories
                .stream()
                .map(h -> new TicketHistoryResponse(
                        h.getId(),
                        h.getTicket().getId(),
                        h.getActor().getId(),
                        h.getActor().getFullName(),
                        h.getAction(),
                        h.getFieldChanged(),
                        h.getOldValue(),
                        h.getNewValue(),
                        h.getReasonOrNote(),
                        h.getTimestamp(),
                        userLabel(h, h.getOldValue(), userNames),
                        userLabel(h, h.getNewValue(), userNames)
                ))
                .toList();
    }

    private static void addUserId(java.util.Set<Long> ids, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            ids.add(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            // Giá trị không phải id thì bỏ qua
        }
    }

    /** Tên người dùng cho old/newValue của dòng phân công; field khác trả null. */
    private static String userLabel(TicketHistory h, String value, java.util.Map<Long, String> names) {
        if (!"current_assignee_id".equals(h.getFieldChanged()) || value == null || value.isBlank()) {
            return null;
        }
        try {
            return names.get(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------

 /**
 * Ticket có tồn tại nhưng người gọi không được xem: ghi AUTHZ_DENIED rồi trả 404 như ticket không tồn tại.
 * Không ghi khi ticket thật sự không tồn tại.
 */
    private AppException denyTicketAccess(User user, String roleName, Ticket ticket, String action) {
        auditLogger.log("AUTHZ_DENIED", user.getId(), roleName, "TICKET", ticket.getTicketNumber(), action, "DENIED", 404);
        return new AppException(ErrorCode.TICKET_NOT_FOUND);
    }

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
                ticket.getCurrentAssignee() != null ? ticket.getCurrentAssignee().getId() : null,
                ticket.getCurrentAssignee() != null ? ticket.getCurrentAssignee().getFullName() : null,
                ticket.getResponseDeadline(),
                ticket.getResolutionDeadline(),
                ticket.getIsSlaBreached(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
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
