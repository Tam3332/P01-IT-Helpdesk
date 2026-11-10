package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request cập nhật trạng thái ticket (PATCH /api/v1/tickets/{id}/status).
 */
public record UpdateTicketStatusRequest(

        @NotBlank(message = "Trạng thái không được để trống")
        String status,

 /** Bắt buộc >= 20 ký tự khi chuyển sang RESOLVED */
        String resolutionNote

) {}
