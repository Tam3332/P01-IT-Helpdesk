package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.UpdateSLAConfigRequest;
import com.ojt.p01.dto.response.SLAConfigResponse;
import com.ojt.p01.service.SLAConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản trị SLA Config.
 *
 * GET /api/v1/admin/sla-configs — IT_MANAGER + ADMIN xem
 * GET /api/v1/admin/sla-configs/{priority} — IT_MANAGER + ADMIN xem theo priority
 * PUT /api/v1/admin/sla-configs/{priority} — ADMIN only chỉnh giờ SLA
 *
 * Phân quyền: Spring Security @PreAuthorize (xem SecurityConfig).
 * Employee / IT_STAFF gọi /admin/** → 403 FORBIDDEN tự động.
 */
@Tag(name = "Categories & SLA", description = "Quản trị danh mục và cấu hình SLA (Admin)")
@RestController
@RequestMapping("/api/v1/admin/sla-configs")
@SecurityRequirement(name = "BearerAuth")
public class SLAConfigController {

    private final SLAConfigService slaConfigService;

    public SLAConfigController(SLAConfigService slaConfigService) {
        this.slaConfigService = slaConfigService;
    }

    @Operation(summary = "[Manager/Admin] Xem toàn bộ cấu hình SLA",
               description = "Trả về 4 mức: CRITICAL → HIGH → MEDIUM → LOW. giờ liên tục 24/7.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SLAConfigResponse>>> getAllSLAConfigs() {
        return ResponseEntity.ok(ApiResponse.success(slaConfigService.getAllSLAConfigs()));
    }

    @Operation(summary = "[Manager/Admin] Xem cấu hình SLA theo priority",
               description = "priority: LOW | MEDIUM | HIGH | CRITICAL")
    @GetMapping("/{priority}")
    public ResponseEntity<ApiResponse<SLAConfigResponse>> getSLAConfigByPriority(
            @Parameter(description = "Mức ưu tiên: LOW | MEDIUM | HIGH | CRITICAL")
            @PathVariable String priority) {
        return ResponseEntity.ok(ApiResponse.success(slaConfigService.getSLAConfigByPriority(priority)));
    }

    @Operation(summary = "[Admin] Cập nhật cấu hình SLA theo priority",
               description = "Chỉnh thời gian phản hồi và giải quyết. Giờ là giờ liên tục (không phải giờ làm việc). Admin only.")
    @PutMapping("/{priority}")
    public ResponseEntity<ApiResponse<SLAConfigResponse>> updateSLAConfig(
            @Parameter(description = "Mức ưu tiên: LOW | MEDIUM | HIGH | CRITICAL")
            @PathVariable String priority,
            @Valid @RequestBody UpdateSLAConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật cấu hình SLA thành công",
                slaConfigService.updateSLAConfig(priority, request)));
    }
}
