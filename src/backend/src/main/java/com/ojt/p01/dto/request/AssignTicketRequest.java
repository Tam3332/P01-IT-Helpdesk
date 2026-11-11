package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của PUT /api/v1/tickets/{id}/assign.
 * Trường khác trong body (status, requesterId...) bị bỏ qua vì record không có field đó.
 */
public record AssignTicketRequest(
        @NotNull(message = "Phải chọn IT Staff được phân công")
        Long assigneeId,

        // Không bắt buộc: Manager xác nhận hoặc điều chỉnh mức ưu tiên chính thức
        @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "priority phải là LOW, MEDIUM, HIGH hoặc CRITICAL")
        String priority,

        // Cột assignment_note VARCHAR(255)
        @Size(max = 255, message = "Ghi chú phân công tối đa 255 ký tự")
        String assignmentNote
) {}
