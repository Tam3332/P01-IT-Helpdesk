package com.ojt.p01.exception;

import com.ojt.p01.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý tập trung tất cả exception của P01 backend.
 *
 * Quy tắc (theo + //):
 * AccessDenied → 403 FORBIDDEN (không 401)
 * BadCredentials → 401 INVALID_CREDENTIALS
 * AppException → httpStatus của ErrorCode tương ứng
 * Validation (400) → có thêm error.details là map field→message
 * Exception chung → 500, KHÔNG lộ message nội bộ ra ngoài
 */
@Slf4j
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
 * Trước đây trả 401 — sửa theo yêu cầu Cường.
 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode ec = ErrorCode.FORBIDDEN;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // -------------------------------------------------------------------------
    // Request sai định dạng — 400 (JSON hỏng, sai kiểu tham số, thiếu tham số/header)
    // Trước đây rơi xuống 500; chỉ dùng 400 cho dữ liệu đầu vào không hợp lệ.
    // -------------------------------------------------------------------------

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        ApiResponse<Object> body = ApiResponse.error(ErrorCode.VALIDATION_FAILED.getCode(),
                "Dữ liệu gửi lên không đúng định dạng");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // Fallback — 500 Internal Server Error (KHÔNG lộ message nội bộ)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        // Lỗi web chuẩn của Spring (URL không tồn tại → 404, sai method → 405...) đã mang sẵn mã HTTP:
        // giữ nguyên mã đó thay vì biến thành 500 (ví dụ API chưa làm phải ra 404, không phải "lỗi hệ thống").
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            String code = status.value() == 404 ? ErrorCode.RESOURCE_NOT_FOUND.getCode()
                    : (HttpStatus.resolve(status.value()) != null ? HttpStatus.resolve(status.value()).name() : "ERROR");
            String message = status.value() == 404 ? ErrorCode.RESOURCE_NOT_FOUND.getMessage()
                    : "Yêu cầu không được hỗ trợ";
            return ResponseEntity.status(status).body(ApiResponse.error(code, message));
        }

        // Log để debug nhưng KHÔNG trả message nội bộ ra client
        log.error("Unhandled exception", ex);
        ErrorCode ec = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse<Object> body = ApiResponse.error(ec.getCode(), ec.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
