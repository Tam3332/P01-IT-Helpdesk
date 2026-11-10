package com.ojt.p01.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu phê duyệt mở lại ticket của IT Manager")
public record ApproveReopenRequest(
        @Schema(description = "Ghi chú phê duyệt của IT Manager (tùy chọn)", example = "Đồng ý mở lại vé để kỹ thuật viên xử lý triệt để sự cố")
        @Size(max = 1000, message = "Ghi chú duyệt tối đa 1000 ký tự")
        String reviewNote,

        @Schema(description = "ID của IT Staff mới nếu muốn phân công lại người khác (tùy chọn theo )", example = "2")
        Long newAssigneeId
) {}
