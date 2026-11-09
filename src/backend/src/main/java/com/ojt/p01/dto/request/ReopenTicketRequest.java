package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body gửi yêu cầu mở lại (POST /api/v1/tickets/{id}/reopen, HD10, UC-08, BR-HD-04).
 * Chỉ nhận reopenReason; status, reviewedBy... không nhận từ client (SDD §6 allowlist).
 * Độ dài tối thiểu 20 kiểm thêm sau khi trim ở ReopenService; tối đa 2000 theo giới hạn bình luận.
 */
public record ReopenTicketRequest(

        @NotBlank(message = "Lý do mở lại không được để trống")
        @Size(min = 20, max = 2000, message = "Lý do mở lại phải từ 20 đến 2000 ký tự")
        String reopenReason

) {}
