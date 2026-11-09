package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.ReopenTicketRequest;
import com.ojt.p01.dto.response.ReopenRequestResponse;
import com.ojt.p01.service.ReopenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mở lại ticket (HD10, UC-08 Nhánh B, BR-HD-04).
 * Quyền theo SecurityConfig: POST /tickets/{id}/reopen chỉ EMPLOYEE; /reopen-requests/** chỉ IT_MANAGER (SDD §6.2).
 */
@Tag(name = "Reopen", description = "Yêu cầu mở lại ticket và danh sách chờ IT Manager duyệt")
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
public class ReopenController {

    private final ReopenService reopenService;

    public ReopenController(ReopenService reopenService) {
        this.reopenService = reopenService;
    }

    @Operation(
            summary = "Gửi yêu cầu mở lại ticket (HD10, UC-08)",
            description = "Chỉ Employee chủ ticket; ticket RESOLVED trong 7 ngày (168 giờ); lý do >= 20 ký tự. Ticket chuyển REOPEN_PENDING."
    )
    @PostMapping("/tickets/{id}/reopen")
    public ResponseEntity<ApiResponse<ReopenRequestResponse>> requestReopen(
            @Parameter(description = "ID số hoặc mã vé") @PathVariable String id,
            @Valid @RequestBody ReopenTicketRequest request,
            HttpServletRequest httpRequest
    ) {
        ReopenRequestResponse response = reopenService.requestReopen(id, request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi yêu cầu mở lại, chờ IT Manager duyệt", response));
    }

    @Operation(
            summary = "Danh sách yêu cầu mở lại (IT Manager, HD10)",
            description = "Mặc định trả các yêu cầu PENDING, kèm giải pháp cũ (previousResolutionNote) để đối chiếu."
    )
    @GetMapping("/reopen-requests")
    public ResponseEntity<ApiResponse<List<ReopenRequestResponse>>> getReopenRequests(
            @Parameter(description = "PENDING | APPROVED | REJECTED") @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(reopenService.getReopenRequests(status)));
    }
}
