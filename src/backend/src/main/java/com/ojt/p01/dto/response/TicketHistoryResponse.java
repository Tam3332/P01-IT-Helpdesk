package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO phản hồi lịch sử xử lý ticket (HD09, UC-02 bước 7, UC-06 bước 6).
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
        OffsetDateTime timestamp
) {}
