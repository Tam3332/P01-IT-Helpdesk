package com.ojt.p01.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request gửi đánh giá chất lượng hỗ trợ (HD15).
 */
public record CreateFeedbackRequest(
        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Số sao tối thiểu là 1")
        @Max(value = 5, message = "Số sao tối đa là 5")
        Integer ratingStars,

        @Size(max = 500, message = "Ý kiến đánh giá không được vượt quá 500 ký tự")
        String feedbackComment
) {}
