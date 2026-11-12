package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO phản hồi lịch sử xử lý ticket.
 */
public record TicketHistoryResponse(
        Long id,
        Long ticketId,
        Long actorId,
        String actorName,
        String action,
        String fieldChanged,
        String oldValue,
        String newValue,
        String reasonOrNote,
        OffsetDateTime timestamp,
        // Tên hiển thị cho old/newValue khi giá trị là id người dùng (field current_assignee_id); null với field khác
        String oldValueLabel,
        String newValueLabel
) {}
