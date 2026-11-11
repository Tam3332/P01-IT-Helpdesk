package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request gửi bình luận (POST /api/v1/tickets/{id}/comments).
 */
public record CreateCommentRequest(

        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
        String commentText,

 /** True nếu là ghi chú nội bộ (chỉ Staff/Manager thấy) */
        Boolean isInternal

) {}
