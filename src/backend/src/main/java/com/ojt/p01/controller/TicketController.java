package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý vòng đời Ticket.
 * Base path: /api/v1/tickets
 */
@Tag(name = "Tickets", description = "Quản lý phiếu yêu cầu hỗ trợ IT")
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * POST /api/v1/tickets
     * Tạo ticket mới. Tự động sinh mã HD-YYYY-XXXX và tính deadline SLA.
     */
    @Operation(
            summary = "Tạo ticket mới (Task T3)",
            description = "Người dùng tạo phiếu yêu cầu hỗ trợ IT. " +
                          "Hệ thống tự động sinh mã vé HD-YYYY-XXXX và tính deadline SLA."
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
     * GET /api/v1/tickets
     * Lấy danh sách ticket có phân trang và bộ lọc:
     * - EMPLOYEE: chỉ xem được vé của mình.
     * - IT_STAFF / IT_MANAGER / ADMIN: xem toàn bộ vé.
     */
    @Operation(
            summary = "Lấy danh sách ticket có phân trang (Task T4)",
            description = "Lấy danh sách ticket theo phạm vi quyền hạn (Employee chỉ xem vé của mình, Staff/Manager xem tất cả). " +
                          "Hỗ trợ phân trang và lọc theo trạng thái (status), mức ưu tiên (priority)."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getTickets(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Lọc trạng thái: NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, REOPEN_PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Lọc mức ưu tiên: LOW, MEDIUM, HIGH, CRITICAL")
            @RequestParam(required = false) String priority
    ) {
        PageResponse<TicketResponse> response = ticketService.getTickets(page, size, status, priority);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/tickets/{ticketNumber}
     * Lấy thông tin chi tiết một vé cụ thể.
     * Bảo mật chống IDOR: Employee chỉ xem được vé của chính mình.
     */
    @Operation(
            summary = "Xem chi tiết một ticket (Task T4)",
            description = "Xem thông tin chi tiết của vé theo mã vé (ví dụ: HD-2026-0001). " +
                          "Kiểm soát quyền truy cập chặt chẽ (chống IDOR): Employee không thể xem vé người khác."
    )
    @GetMapping("/{ticketNumber}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketDetail(
            @Parameter(description = "Mã vé định dạng HD-YYYY-XXXX (ví dụ: HD-2026-0001)")
            @PathVariable String ticketNumber
    ) {
        TicketDetailResponse response = ticketService.getTicketDetail(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
