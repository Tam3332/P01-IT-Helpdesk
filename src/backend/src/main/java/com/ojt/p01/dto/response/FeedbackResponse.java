package com.ojt.p01.dto.response;

import java.time.OffsetDateTime;

/**
 * Phản hồi thông tin đánh giá CSAT của ticket (HD15).
 */
public record FeedbackResponse(
        Long id,
        Long ticketId,
        Long requesterId,
        String requesterName,
        Integer ratingStars,
        String feedbackComment,
        OffsetDateTime createdAt
) {}
