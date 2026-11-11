package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.request.CreateCategoryRequest;
import com.ojt.p01.dto.request.UpdateCategoryRequest;
import com.ojt.p01.dto.response.CategoryDetailResponse;
import com.ojt.p01.dto.response.CategoryResponse;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản trị danh mục.
 *
 * Public read: GET /api/v1/categories — tất cả user đã login
 * Admin read: GET /api/v1/admin/categories — ADMIN
 * Admin write: POST/PUT/PATCH /admin/categories — ADMIN only
 *
 * Phân quyền thực hiện trong SecurityConfig (hasRole):
 * Employee / IT_STAFF gọi /admin/** → 403 FORBIDDEN (Spring Security chặn trước khi vào controller)
 */
@Tag(name = "Categories & SLA", description = "Quản trị danh mục và cấu hình SLA (Admin)")
@RestController
@SecurityRequirement(name = "BearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // =========================================================================
    // PUBLIC READ — tất cả user đã login (dùng cho dropdown tạo vé)
    // =========================================================================

    @Operation(summary = "Lấy danh sách danh mục Active (dropdown tạo vé)",
               description = "Trả về các danh mục isActive=true, sắp xếp A-Z. Dùng cho dropdown khi Employee tạo vé.")
    @GetMapping("/api/v1/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }

    // =========================================================================
    // ADMIN — Read toàn bộ (kể cả Inactive)
    // =========================================================================

    @Operation(summary = "[Admin] Xem toàn bộ danh mục kể cả Inactive")
    @GetMapping("/api/v1/admin/categories")
    public ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @Operation(summary = "[Admin] Xem chi tiết 1 danh mục theo id")
    @GetMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> getCategoryById(
            @Parameter(description = "ID danh mục") @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    // =========================================================================
    // ADMIN — Write (Create / Update / Toggle)
    // =========================================================================

    @Operation(summary = "[Admin] Tạo danh mục mới",
               description = "Mã category_code phải duy nhất, chỉ gồm chữ hoa, số và dấu gạch dưới.")
    @PostMapping("/api/v1/admin/categories")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryDetailResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo danh mục thành công", created));
    }

    @Operation(summary = "[Admin] Cập nhật thông tin danh mục",
               description = "Partial update — chỉ field không null trong body mới được cập nhật.")
    @PutMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> updateCategory(
            @Parameter(description = "ID danh mục") @PathVariable Integer id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", categoryService.updateCategory(id, request)));
    }

    @Operation(summary = "[Admin] Bật / Tắt trạng thái hoạt động danh mục",
               description = "PATCH với body {active: true/false}. Danh mục Inactive sẽ không hiện ở dropdown tạo vé.")
    @PatchMapping("/api/v1/admin/categories/{id}/status")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> toggleCategoryStatus(
            @Parameter(description = "ID danh mục") @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            // Thiếu trường active → 400 (trước đây IllegalArgumentException rơi xuống 500)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Body phải có trường 'active' (true/false)");
        }
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Danh mục đã được kích hoạt" : "Danh mục đã bị vô hiệu hóa",
                categoryService.toggleCategoryStatus(id, active)));
    }
    @Operation(summary = "[Admin] Xóa vĩnh viễn danh mục",
               description = "Chỉ xóa được khi danh mục chưa có ticket tham chiếu. Nếu đang có ticket tham chiếu → 422 Unprocessable Entity.")
    @DeleteMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "ID danh mục") @PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }
}
