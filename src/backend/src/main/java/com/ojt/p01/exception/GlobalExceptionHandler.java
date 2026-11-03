package com.ojt.p01.exception;

import com.ojt.p01.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý tập trung tất cả exception của P01 backend.
 *
 * Quy tắc (theo SDD §4.2 + SRS UC-01/UC-02/UC-10):
 *  - AccessDenied     → 403 FORBIDDEN   (không 401)
 *  - BadCredentials   → 401 INVALID_CREDENTIALS
 *  - AppException     → httpStatus của ErrorCode tương ứng
 *  - Validation (400) → có thêm error.details là map field→message
 *  - Exception chung  → 500, KHÔNG lộ message nội bộ ra ngoài
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // AppException — lỗi nghiệp vụ có định nghĩa sẵn
    // -------------------------------------------------------------------------

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ErrorCode ec = ex.getErrorCode();
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ex.getMessage());
        return ResponseEntity.status(ec.getHttpStatus()).body(body);
    }

    // -------------------------------------------------------------------------
    // Validation — 400 Bad Request + details
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // Security — Auth / Access
    // -------------------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        ErrorCode ec = ErrorCode.INVALID_CREDENTIALS;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * AccessDeniedException: user đã xác thực nhưng không có quyền → 403 FORBIDDEN.
     * Trước đây trả 401 — sửa theo yêu cầu Cường (SRS UC-10).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode ec = ErrorCode.FORBIDDEN;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // -------------------------------------------------------------------------
    // Fallback — 500 Internal Server Error (KHÔNG lộ message nội bộ)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        // Log để debug nhưng KHÔNG trả message nội bộ ra client
        // log.error("Unhandled exception", ex);  // uncomment khi thêm logger
        ErrorCode ec = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
