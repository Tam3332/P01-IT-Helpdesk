package com.ojt.p01.dto.response;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * DTO chuẩn hóa kết quả phân trang cho Frontend React.
 */
public record PageResponse<T>(
        List<T> content,
        int pageNo,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
