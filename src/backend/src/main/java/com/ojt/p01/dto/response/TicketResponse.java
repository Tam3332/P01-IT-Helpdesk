package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * Ticket dạng rút gọn: trả về khi tạo ticket và trong danh sách GET /tickets.
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
 * @param assigneeId         ID IT Staff đang phụ trách (null nếu chưa phân công)
 * @param assigneeName       Tên IT Staff đang phụ trách (cột "Người xử lý" ở FE; null nếu chưa phân công)
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
        Long assigneeId,
        String assigneeName,
        OffsetDateTime responseDeadline,
        OffsetDateTime resolutionDeadline,
        OffsetDateTime createdAt
) {}
