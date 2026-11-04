package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.entity.TicketPriority;
import com.ojt.p01.entity.TicketStatus;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            @RequestParam(required = false) String priority
    ) {
        PageResponse<TicketResponse> response = ticketService.getTickets(page, size, status, priority);
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
