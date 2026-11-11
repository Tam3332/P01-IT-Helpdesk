package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO phản hồi thông tin tệp đính kèm.
 */
public record AttachmentResponse(
        Long id,
        Long ticketId,
        String fileName,
        Long fileSize,
        String mimeType,
        Long uploaderId,
        String uploaderName,
        OffsetDateTime createdAt
) {}
