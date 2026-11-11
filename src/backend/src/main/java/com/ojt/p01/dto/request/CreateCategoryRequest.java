package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO tạo danh mục mới (POST /api/v1/admin/categories).
 * Quyền: ADMIN only.
 */
public record CreateCategoryRequest(

        @NotBlank(message = "Mã danh mục không được để trống")
        @Size(max = 50, message = "Mã danh mục tối đa 50 ký tự")
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã danh mục chỉ gồm chữ hoa, số và dấu gạch dưới")
        String categoryCode,

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
        String categoryName,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description,

        @NotBlank(message = "Mức ưu tiên mặc định không được để trống")
        @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "defaultPriority phải là LOW, MEDIUM, HIGH hoặc CRITICAL")
        String defaultPriority
) {}
