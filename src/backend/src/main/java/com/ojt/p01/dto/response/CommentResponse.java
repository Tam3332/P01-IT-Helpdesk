package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO phản hồi thông tin bình luận.
 */
public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        String authorRole,
        String commentText,
        Boolean isInternal,
        OffsetDateTime createdAt
) {}
