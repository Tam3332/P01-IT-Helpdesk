package com.ojt.p01.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body để tạo ticket mới (POST /api/v1/tickets).
 *
 * @param title       Tiêu đề ticket (bắt buộc, tối đa 200 ký tự)
 * @param description Mô tả chi tiết sự cố (bắt buộc, tối đa 2000 ký tự)
 * @param categoryId  ID danh mục sự cố (lấy từ GET /api/v1/categories)
 * @param impactLevel Mức độ ảnh hưởng: LOW | MEDIUM | HIGH | CRITICAL
 */
public record CreateTicketRequest(

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề không vượt quá 200 ký tự")
        String title,

        @NotBlank(message = "Mô tả không được để trống")
        @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự")
        String description,

        @NotNull(message = "Danh mục không được để trống")
        Integer categoryId,

        @NotBlank(message = "Mức độ ảnh hưởng không được để trống")
        @Pattern(
                regexp = "LOW|MEDIUM|HIGH|CRITICAL",
                message = "Mức độ ảnh hưởng phải là: LOW, MEDIUM, HIGH hoặc CRITICAL"
        )
        String impactLevel

) {}
