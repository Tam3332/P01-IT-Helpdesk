package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.response.CategoryResponse;
import com.ojt.p01.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller quản lý danh mục sự cố.
 * Base path: /api/v1/categories
 */
@Tag(name = "Categories", description = "Danh mục sự cố / yêu cầu hỗ trợ IT")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * GET /api/v1/categories
     * Lấy danh sách danh mục đang hoạt động để hiển thị dropdown tạo ticket.
     */
    @Operation(
            summary = "Lấy danh sách danh mục sự cố",
            description = "Trả về các danh mục is_active = true, sắp xếp A-Z theo tên."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }
}
