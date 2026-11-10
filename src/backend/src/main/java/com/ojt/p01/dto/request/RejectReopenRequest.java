package com.ojt.p01.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu từ chối mở lại ticket của IT Manager (HD10, BPMN P3)")
public record RejectReopenRequest(
        @Schema(description = "Lý do từ chối mở lại (bắt buộc theo SRS HD10 / BR-HD-04)", example = "Sự cố mạng đã được khắc phục hoàn toàn theo biên bản, vấn đề mới phát sinh vui lòng tạo ticket riêng")
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(min = 10, max = 1000, message = "Lý do từ chối phải từ 10 đến 1000 ký tự")
        String reviewNote
) {}
