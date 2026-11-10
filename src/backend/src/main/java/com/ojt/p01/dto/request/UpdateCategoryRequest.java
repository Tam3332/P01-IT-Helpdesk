package com.ojt.p01.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO cập nhật danh mục (PUT /api/v1/admin/categories/{id}).
 * Tất cả field nullable — chỉ field nào không null mới được cập nhật (partial update).
 * Quyền: ADMIN only.
 */
public record UpdateCategoryRequest(

        @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
        String categoryName,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description,

        @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "defaultPriority phải là LOW, MEDIUM, HIGH hoặc CRITICAL")
        String defaultPriority
) {}
