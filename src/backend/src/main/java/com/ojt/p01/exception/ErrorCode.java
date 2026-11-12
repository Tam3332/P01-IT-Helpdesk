package com.ojt.p01.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ của P01 (chuẩn hóa mã lỗi nghiệp vụ).
 * error.code trả về đúng tên chuỗi chuẩn: CATEGORY_NOT_FOUND, INVALID_CREDENTIALS...
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
    SLA_CONFIG_NOT_FOUND   ("SLA_CONFIG_NOT_FOUND", "Không tìm thấy cấu hình SLA cho mức ưu tiên này", HttpStatus.NOT_FOUND),

    // -------------------------------------------------------------------------
    // Auth / User
    // -------------------------------------------------------------------------
    INVALID_CREDENTIALS        ("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED           ("ACCOUNT_DISABLED", "Tài khoản đã bị khóa, vui lòng liên hệ Admin", HttpStatus.FORBIDDEN),
    INVALID_CURRENT_PASSWORD   ("INVALID_CURRENT_PASSWORD", "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_CURRENT   ("PASSWORD_SAME_AS_CURRENT", "Mật khẩu mới không được trùng với mật khẩu hiện tại", HttpStatus.BAD_REQUEST),

    // -------------------------------------------------------------------------
    // Ticket state machine
    // -------------------------------------------------------------------------
    TICKET_NOT_ASSIGNABLE      ("TICKET_NOT_ASSIGNABLE", "Ticket không ở trạng thái cho phép phân công", HttpStatus.UNPROCESSABLE_ENTITY), // 422: tiền điều kiện; (Closed → 422)
    ASSIGNEE_UNCHANGED         ("ASSIGNEE_UNCHANGED", "IT Staff này đang phụ trách ticket", HttpStatus.UNPROCESSABLE_ENTITY), // 422:
    INVALID_ASSIGNEE           ("INVALID_ASSIGNEE", "Người được giao phải là IT Staff đang hoạt động", HttpStatus.UNPROCESSABLE_ENTITY), // 422:
    INVALID_STATUS_TRANSITION  ("INVALID_STATUS_TRANSITION", "Chuyển trạng thái vé không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY), // 422: , bước 5
    REOPEN_WINDOW_EXPIRED      ("REOPEN_WINDOW_EXPIRED", "Đã quá thời hạn 07 ngày kể từ khi giải quyết sự cố, vui lòng tạo phiếu yêu cầu mới", HttpStatus.UNPROCESSABLE_ENTITY), // 422: , /reopen "quá 7 ngày"
    REOPEN_ALREADY_PENDING     ("REOPEN_ALREADY_PENDING", "Ticket đã có yêu cầu mở lại đang chờ phê duyệt", HttpStatus.UNPROCESSABLE_ENTITY), // 422: /reopen "đang có yêu cầu chờ duyệt" (nhóm không dùng 409)

    // -------------------------------------------------------------------------
    // Admin — Category & SLAConfig
    // -------------------------------------------------------------------------
    CATEGORY_CODE_DUPLICATE    ("CATEGORY_CODE_DUPLICATE", "Mã danh mục đã tồn tại", HttpStatus.BAD_REQUEST), // 400: (nhóm không dùng 409)
    CATEGORY_NAME_DUPLICATE    ("CATEGORY_NAME_DUPLICATE", "Tên danh mục đã tồn tại", HttpStatus.BAD_REQUEST), // 400: "trùng mã/tên", "tên duy nhất"
    CATEGORY_ID_NOT_FOUND      ("CATEGORY_ID_NOT_FOUND", "Không tìm thấy danh mục", HttpStatus.NOT_FOUND), // 404: Admin gọi /admin/categories/{id} sai id
    CATEGORY_HAS_TICKETS       ("CATEGORY_HAS_TICKETS", "Danh mục đang có phiếu yêu cầu tham chiếu, không thể xóa", HttpStatus.UNPROCESSABLE_ENTITY), // 422:
    SLA_INVALID_HOURS          ("SLA_INVALID_HOURS", "Thời gian SLA phải lớn hơn 0", HttpStatus.BAD_REQUEST),

    // -------------------------------------------------------------------------
    // Comments & Attachments & Lifecycle
    // -------------------------------------------------------------------------
    RESOLUTION_NOTE_REQUIRED   ("RESOLUTION_NOTE_REQUIRED", "Giải pháp kỹ thuật bắt buộc khi hoàn tất xử lý (tối thiểu 20 ký tự)", HttpStatus.BAD_REQUEST),
    TICKET_CLOSED              ("TICKET_CLOSED", "Ticket đã đóng, không thể thao tác", HttpStatus.UNPROCESSABLE_ENTITY),
    TICKET_NOT_RESOLVED        ("TICKET_NOT_RESOLVED", "Chỉ có thể đóng ticket khi đang ở trạng thái RESOLVED", HttpStatus.UNPROCESSABLE_ENTITY),
    FILE_TOO_LARGE             ("FILE_TOO_LARGE", "Dung lượng tệp vượt quá giới hạn 10MB", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED      ("FILE_TYPE_NOT_ALLOWED", "Định dạng tệp không được hỗ trợ", HttpStatus.BAD_REQUEST),
    ATTACHMENT_LIMIT_EXCEEDED  ("ATTACHMENT_LIMIT_EXCEEDED", "Mỗi ticket chỉ được đính kèm tối đa 5 tệp", HttpStatus.BAD_REQUEST),
    ATTACHMENT_NOT_FOUND       ("ATTACHMENT_NOT_FOUND", "Không tìm thấy tệp đính kèm", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
