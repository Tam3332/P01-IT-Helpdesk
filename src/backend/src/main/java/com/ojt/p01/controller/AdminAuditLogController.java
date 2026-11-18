package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.response.AuditLogResponse;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tra cứu nhật ký kiểm toán bất biến (Audit Logs) cho Quản trị viên (Admin only).
 * Base path: /api/v1/admin/audit-logs
 *
 * Phân quyền: Spring Security tự động chặn role khác (403 FORBIDDEN + AUTHZ_DENIED).
 */
@Tag(name = "Admin & Audit", description = "Quản trị nhật ký hệ thống (Admin only)")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@SecurityRequirement(name = "BearerAuth")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * GET /api/v1/admin/audit-logs
     * Lấy danh sách nhật ký hệ thống có phân trang và bộ lọc.
     */
    @Operation(summary = "Xem nhật ký kiểm toán hệ thống (ADMIN)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Từ khóa tìm kiếm (họ tên, hành động, đối tượng)") @RequestParam(required = false) String search,
            @Parameter(description = "Lọc kết quả: ok (Thành công) hoặc fail (Thất bại)") @RequestParam(required = false) String result
    ) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAuditLogs(page, size, search, result)));
    }
}
