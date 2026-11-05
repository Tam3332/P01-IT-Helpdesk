package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO trả về thông tin đầy đủ của danh mục (dùng cho Admin view).
 */
public record CategoryDetailResponse(
        Integer id,
        String categoryCode,
        String categoryName,
        String description,
        String defaultPriority,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
