package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * Response trả về sau khi tạo ticket thành công.
 *
 * @param id                 ID nội bộ
 * @param ticketNumber       Mã vé: HD-YYYY-XXXX
 * @param title              Tiêu đề
 * @param description        Mô tả
 * @param categoryName       Tên danh mục
 * @param impactLevel        Mức độ ảnh hưởng
 * @param priority           Mức ưu tiên (= impactLevel ở bước này)
 * @param status             Trạng thái (NEW)
 * @param requesterName      Tên người tạo
 * @param responseDeadline   Hạn phản hồi lần đầu (SLA)
 * @param resolutionDeadline Hạn giải quyết xong (SLA)
 * @param createdAt          Thời điểm tạo
 */
public record TicketResponse(
        Long id,
        String ticketNumber,
        String title,
        String description,
        String categoryName,
        String impactLevel,
        String priority,
        String status,
        String requesterName,
        OffsetDateTime responseDeadline,
        OffsetDateTime resolutionDeadline,
        OffsetDateTime createdAt
) {}
