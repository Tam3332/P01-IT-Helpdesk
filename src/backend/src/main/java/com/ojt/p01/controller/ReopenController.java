package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.ApproveReopenRequest;
import com.ojt.p01.dto.request.RejectReopenRequest;
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
 * Mở lại ticket.
 * Quyền theo SecurityConfig:
 * POST /tickets/{id}/reopen chỉ EMPLOYEE (chủ vé)
 * /reopen-requests/** chỉ IT_MANAGER
 */
@Tag(name = "Reopen", description = "Yêu cầu mở lại ticket và quy trình IT Manager duyệt/từ chối")
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
public class ReopenController {

    private final ReopenService reopenService;

    public ReopenController(ReopenService reopenService) {
        this.reopenService = reopenService;
    }

    @Operation(
            summary = "Gửi yêu cầu mở lại ticket",
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
            summary = "Danh sách yêu cầu mở lại (IT Manager)",
            description = "Mặc định trả các yêu cầu PENDING, kèm giải pháp cũ (previousResolutionNote) để đối chiếu."
    )
    @GetMapping("/reopen-requests")
    public ResponseEntity<ApiResponse<List<ReopenRequestResponse>>> getReopenRequests(
            @Parameter(description = "PENDING | APPROVED | REJECTED") @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(reopenService.getReopenRequests(status)));
    }

    @Operation(
            summary = "Phê duyệt yêu cầu mở lại ticket (IT Manager)",
            description = "Chuyển ticket sang REOPENED, kích hoạt SLA xử lý mới, tăng reopen_count, có thể chọn phân công lại IT Staff mới."
    )
    @PostMapping({"/reopen-requests/{id}/approve", "/reopen-requests/{requestId}/approve"})
    public ResponseEntity<ApiResponse<ReopenRequestResponse>> approveReopen(
            @Parameter(description = "ID của yêu cầu ReopenRequest") @PathVariable(value = "id", required = false) Long id,
            @PathVariable(value = "requestId", required = false) Long requestId,
            @Valid @RequestBody(required = false) ApproveReopenRequest request,
            HttpServletRequest httpRequest
    ) {
        Long targetId = id != null ? id : requestId;
        ReopenRequestResponse response = reopenService.approveReopenRequest(targetId, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt mở lại ticket thành công", response));
    }

    @Operation(
            summary = "Từ chối yêu cầu mở lại ticket (IT Manager)",
            description = "Bắt buộc có reviewNote (lý do từ chối), chuyển ticket quay lại trạng thái RESOLVED."
    )
    @PostMapping({"/reopen-requests/{id}/reject", "/reopen-requests/{requestId}/reject"})
    public ResponseEntity<ApiResponse<ReopenRequestResponse>> rejectReopen(
            @Parameter(description = "ID của yêu cầu ReopenRequest") @PathVariable(value = "id", required = false) Long id,
            @PathVariable(value = "requestId", required = false) Long requestId,
            @Valid @RequestBody RejectReopenRequest request,
            HttpServletRequest httpRequest
    ) {
        Long targetId = id != null ? id : requestId;
        ReopenRequestResponse response = reopenService.rejectReopenRequest(targetId, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu mở lại ticket", response));
    }
}
