package com.ojt.p01.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Wrapper chuẩn cho mọi HTTP response của P01 backend.
 *
 * Success: { success: true, message, data, timestamp }
 * Error: { success: false, message, error: { code, message, details }, timestamp }
 *
 * Ref:
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

 /** Chỉ có khi success=false. Ref: ErrorResponse inner class bên dưới. */
    private ErrorDetail error;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Factory — Success
    // -------------------------------------------------------------------------

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // -------------------------------------------------------------------------
    // Factory — Error (dùng trong GlobalExceptionHandler)
    // -------------------------------------------------------------------------

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorMessage)
                .error(ErrorDetail.of(errorCode, errorMessage, null))
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorMessage)
                .error(ErrorDetail.of(errorCode, errorMessage, details))
                .build();
    }

    // -------------------------------------------------------------------------
    // Inner class — ErrorDetail
    // -------------------------------------------------------------------------

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
 /** Mã lỗi nghiệp vụ, ví dụ: INVALID_CREDENTIALS, CATEGORY_NOT_FOUND */
        private String code;
        private String message;
 /** Danh sách lỗi validation field (chỉ có khi 400 Validation Failed) */
        private Object details;

        public static ErrorDetail of(String code, String message, Object details) {
            return ErrorDetail.builder()
                    .code(code)
                    .message(message)
                    .details(details)
                    .build();
        }
    }
}
