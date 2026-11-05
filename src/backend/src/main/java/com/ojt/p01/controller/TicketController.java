package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.AssignTicketRequest;
import com.ojt.p01.dto.request.UpdateTicketStatusRequest;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.entity.TicketPriority;
import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.dto.response.TicketHistoryResponse;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Controller quản lý vòng đời Ticket (HD03, HD05).
 * Base path: /api/v1/tickets
 */
@Tag(name = "Tickets", description = "Quản lý phiếu yêu cầu hỗ trợ IT")
@RestController
@RequestMapping("/api/v1/tickets")
@SecurityRequirement(name = "BearerAuth")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * POST /api/v1/tickets (HD03, UC-02)
     * Tạo ticket mới. Chỉ dành cho EMPLOYEE. Role khác nhận 403.
     */
    @Operation(
            summary = "Tạo ticket mới (HD03, UC-02)",
            description = "Chỉ nhân viên (EMPLOYEE) được tạo phiếu. Hệ thống tự động sinh mã vé HD-YYYY-XXXX và tính deadline SLA."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {

        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket created successfully", response));
    }

    /**
     * GET /api/v1/tickets (HD05, UC-04)
     * Lấy danh sách ticket có phân trang và bộ lọc:
     * - EMPLOYEE: chỉ xem vé của mình.
     * - IT_STAFF: xem vé được giao + vé NEW.
     * - IT_MANAGER / ADMIN: xem toàn bộ vé.
     */
    @Operation(
            summary = "Lấy danh sách ticket có phân trang (HD05, UC-04)",
            description = "Phân quyền theo Access Matrix: Employee chỉ thấy vé của mình, Staff thấy vé được giao + NEW, Manager/Admin thấy tất cả. " +
                          "Mặc định kích thước trang là 20 bản ghi."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getTickets(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang (mặc định 20)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Lọc trạng thái: NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, REOPEN_PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Lọc mức ưu tiên: LOW, MEDIUM, HIGH, CRITICAL")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Lọc theo ID danh mục") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "Lọc theo ID IT Staff đang phụ trách") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Tìm theo tiêu đề hoặc mô tả (không phân biệt hoa thường)") @RequestParam(required = false) String search
    ) {
        // Bộ lọc chỉ thu hẹp kết quả trong phạm vi role (SecRS AC-05)
        PageResponse<TicketResponse> response = ticketService.getTickets(page, size, status, priority, categoryId, assigneeId, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/tickets/{id} (HD05, UC-04, IA-HD02)
     * Lấy chi tiết ticket theo ID số (ví dụ: 1) hoặc mã vé (ví dụ: HD-2026-0001).
     * Áp dụng cơ chế Anti-IDOR: nếu không có quyền truy cập, trả về 404 Not Found.
     */
    @Operation(
            summary = "Xem chi tiết một ticket (HD05, UC-04, IA-HD02)",
            description = "Hỗ trợ tra cứu theo ID số hoặc mã vé HD-YYYY-XXXX. Áp dụng Anti-IDOR: Employee xem vé người khác hoặc Staff xem vé ngoài phạm vi nhận 404 Not Found."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketDetail(
            @Parameter(description = "ID số của vé (ví dụ: 1) hoặc mã vé (ví dụ: HD-2026-0001)")
            @PathVariable String id
    ) {
        TicketDetailResponse response = ticketService.getTicketDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/tickets/{id}/assign (HD06, UC-06) — chỉ IT_MANAGER.
     * 200: ticket sau phân công | 400: body sai | 401 | 403: không phải IT_MANAGER | 404: không có ticket
     * 422: ticket không ở trạng thái phân công được, hoặc người được giao không phải IT Staff đang hoạt động.
     */
    @Operation(
            summary = "Phân công / điều phối lại người xử lý (HD06, UC-06)",
            description = "Body: assigneeId (bắt buộc), priority (tùy chọn), assignmentNote (tùy chọn, ≤ 255 ký tự)."
    )
    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> assignTicket(
            @Parameter(description = "ID số hoặc mã vé") @PathVariable String id,
            @Valid @RequestBody AssignTicketRequest request,
            HttpServletRequest httpRequest
    ) {
        // IP người thao tác cho ticket_history.ip_address (NOT NULL). Khi chạy sau Nginx cần cấu hình đọc X-Forwarded-For.
        TicketDetailResponse response = ticketService.assignTicket(id, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Phân công thành công", response));
    }

    /**
     * PATCH /api/v1/tickets/{id}/status (HD07, UC-07, BR-HD-01)
     * IT Staff nhận xử lý (IN_PROGRESS) hoặc hoàn tất sự cố (RESOLVED).
     * Body: status (IN_PROGRESS | RESOLVED), resolutionNote (bắt buộc khi RESOLVED, >= 20 ký tự).
     */
    @Operation(
            summary = "Cập nhật trạng thái xử lý ticket (HD07, UC-07)",
            description = "Chỉ IT Staff đang được giao hoặc IT Manager. Chuyển sang RESOLVED bắt buộc có resolutionNote >= 20 ký tự (BR-HD-01)."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> updateTicketStatus(
            @Parameter(description = "ID số hoặc mã véc") @PathVariable String id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        TicketDetailResponse response = ticketService.updateTicketStatus(id, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", response));
    }

    /**
     * POST /api/v1/tickets/{id}/close (HD10, UC-08, BR-HD-02, BR-HD-04)
     * Xác nhận đóng ticket hoàn tất. Chỉ áp dụng khi ticket đang ở trạng thái RESOLVED.
     */
    @Operation(
            summary = "Xác nhận đóng ticket hoàn tất (HD10, UC-08)",
            description = "Chỉ Employee chủ ticket hoặc IT Manager được đóng (BR-HD-02). Chỉ đóng được khi trạng thái là RESOLVED."
    )
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> closeTicket(
            @Parameter(description = "ID số hoặc mã véc") @PathVariable String id,
            HttpServletRequest httpRequest
    ) {
        TicketDetailResponse response = ticketService.closeTicket(id, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Đóng ticket thành công", response));
    }


    /**
     * GET /api/v1/tickets/{id}/history (HD09, UC-02 bước 7, UC-06 bước 6)
     * Lấy toàn bộ lịch sử biến động / audit trail của ticket theo thứ tự thời gian.
     * Chống IDOR (IA-HD02): Employee chỉ xem lịch sử ticket của mình; Staff xem ticket được giao hoặc NEW; Manager/Admin xem tất cả.
     */
    @Operation(
            summary = "Xem lịch sử xử lý ticket (HD09)",
            description = "Trả về danh sách các sự kiện (CREATE, ASSIGN, REASSIGN...) theo thứ tự thời gian tăng dần."
    )
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<TicketHistoryResponse>>> getTicketHistory(
            @Parameter(description = "ID số hoặc mã véc") @PathVariable String id
    ) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketHistory(id)));
    }


    /**
     * GET /api/v1/tickets/priorities (HD02)
     * Danh sách mức ưu tiên (enum) để frontend hiển thị dropdown.
     */
    @Operation(
            summary = "Danh sách mức ưu tiên (HD02)",
            description = "Trả về các giá trị enum TicketPriority: LOW, MEDIUM, HIGH, CRITICAL."
    )
    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse<List<String>>> getPriorities() {
        List<String> values = Arrays.stream(TicketPriority.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success(values));
    }

    /**
     * GET /api/v1/tickets/statuses (HD02)
     * Danh sách trạng thái ticket (enum) để frontend hiển thị dropdown.
     */
    @Operation(
            summary = "Danh sách trạng thái ticket (HD02)",
            description = "Trả về các giá trị enum TicketStatus."
    )
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<String>>> getStatuses() {
        List<String> values = Arrays.stream(TicketStatus.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success(values));
    }

}
