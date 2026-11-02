package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Tạo ticket mới. Yêu cầu đăng nhập (requester lấy từ JWT sau khi Cường xong C4).
     * Trả về 201 Created với thông tin vé vừa tạo.
     */
    @Operation(
            summary = "Tạo ticket mới",
            description = "Employee tạo phiếu yêu cầu hỗ trợ IT. " +
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
}
