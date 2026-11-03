package com.ojt.p01.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ của P01 (khớp evidence T3-06 và SRS UC-01/UC-02/UC-10).
 * error.code trả về đúng tên chuỗi chuẩn: CATEGORY_NOT_FOUND, INVALID_CREDENTIALS,...
 */
@Getter
public enum ErrorCode {

    // -------------------------------------------------------------------------
    // Hệ thống & Chung
    // -------------------------------------------------------------------------
    UNCATEGORIZED_EXCEPTION("UNCATEGORIZED_EXCEPTION", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED      ("VALIDATION_FAILED", "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND     ("RESOURCE_NOT_FOUND", "Tài nguyên không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED        ("UNAUTHENTICATED", "Bạn chưa đăng nhập hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),
    FORBIDDEN              ("FORBIDDEN", "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),

    // -------------------------------------------------------------------------
    // Ticket / Category / SLA
    // -------------------------------------------------------------------------
    TICKET_NOT_FOUND       ("TICKET_NOT_FOUND", "Không tìm thấy phiếu yêu cầu", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND     ("CATEGORY_NOT_FOUND", "Danh mục không tồn tại hoặc đã bị vô hiệu hóa", HttpStatus.BAD_REQUEST),
    SLA_CONFIG_NOT_FOUND   ("SLA_CONFIG_NOT_FOUND", "Không tìm thấy cấu hình SLA cho mức ưu tiên này", HttpStatus.INTERNAL_SERVER_ERROR),

    // -------------------------------------------------------------------------
    // Auth / User
    // -------------------------------------------------------------------------
    INVALID_CREDENTIALS        ("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED           ("ACCOUNT_DISABLED", "Tài khoản đã bị khóa, vui lòng liên hệ Admin", HttpStatus.FORBIDDEN),

    // -------------------------------------------------------------------------
    // Ticket state machine
    // -------------------------------------------------------------------------
    INVALID_STATUS_TRANSITION  ("INVALID_STATUS_TRANSITION", "Chuyển trạng thái vé không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY), // 422: SRS UC-07 2a, SDD §6.5 bước 5

    // -------------------------------------------------------------------------
    // Admin — Category & SLAConfig
    // -------------------------------------------------------------------------
    CATEGORY_CODE_DUPLICATE    ("CATEGORY_CODE_DUPLICATE", "Mã danh mục đã tồn tại", HttpStatus.BAD_REQUEST), // 400: SRS UC-10 3a (nhóm không dùng 409)
    SLA_INVALID_HOURS          ("SLA_INVALID_HOURS", "Thời gian SLA phải lớn hơn 0", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
