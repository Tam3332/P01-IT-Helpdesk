package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.response.DashboardStatsResponse;
import com.ojt.p01.dto.response.SLAMonitorResponse;
import com.ojt.p01.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller cung cấp API Thống kê tổng hợp (HD14) và Giám sát cảnh báo SLA (HD12).
 * Base path: /api/v1/dashboard
 */
@Tag(name = "Dashboard", description = "Thống kê số liệu, hàng đợi xử lý và giám sát cảnh báo SLA (HD12, HD14)")
@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/dashboard/stats
     * Thống kê tổng hợp số lượng vé theo trạng thái, vi phạm SLA, danh mục, kỹ thuật viên.
     * Dữ liệu tự động lọc theo quyền vai trò (Role-based scoping).
     */
    @Operation(summary = "Thống kê tổng hợp Dashboard (HD14)",
            description = "Trả về số vé mới tạo, đang xử lý, quá hạn, hoàn tất; thống kê theo danh mục và theo kỹ thuật viên.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats()));
    }

    /**
     * GET /api/v1/dashboard/sla-monitor
     * Giám sát danh sách vé đang mở sắp chạm hoặc đã vượt ngưỡng cam kết SLA (HD12).
     */
    @Operation(summary = "Giám sát cảnh báo SLA (HD12)",
            description = "Danh sách vé đang mở kèm % thời gian đã sử dụng và nhãn SLA (Normal, Near Breach, Breached).")
    @GetMapping("/sla-monitor")
    public ResponseEntity<ApiResponse<List<SLAMonitorResponse>>> getSlaMonitor(
            @Parameter(description = "Chỉ lấy các vé đã vi phạm SLA")
            @RequestParam(required = false, defaultValue = "false") Boolean breachedOnly
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSlaMonitor(breachedOnly)));
    }
}
