package com.ojt.p01.dto.response;

/**
 * DTO trả về thông tin danh mục sự cố.
 * Dùng cho GET /api/v1/categories (dropdown tạo ticket).
 */
public record CategoryResponse(
        Integer id,
        String categoryCode,
        String categoryName,
        String description,
        String defaultPriority
) {}
