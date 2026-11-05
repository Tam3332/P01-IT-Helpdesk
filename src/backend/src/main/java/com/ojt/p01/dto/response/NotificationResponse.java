package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO thông báo cho icon chuông thông báo (Employee, Staff, Manager).
 * Hiển thị mã ticket, tiêu đề, trạng thái và thời gian.
 */
public record NotificationResponse(
        Long id,
        Long ticketId,
        String ticketNumber,
        String title,
        String status,
        String action,
        String actorName,
        OffsetDateTime timestamp
) {}
