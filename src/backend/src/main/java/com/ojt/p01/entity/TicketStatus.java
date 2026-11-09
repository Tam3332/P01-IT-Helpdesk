package com.ojt.p01.entity;

/**
 * Enum trạng thái vé (type-safe thay cho String tự do).
 *
 * Bảng chuyển trạng thái hợp lệ (State Machine — BR-HD-01, BR-HD-04):
 *
 *  NEW            → ASSIGNED        (IT_MANAGER phân công — HD06)
 *  ASSIGNED       → IN_PROGRESS     (IT_STAFF nhận xử lý — HD07)
 *  IN_PROGRESS    → RESOLVED        (IT_STAFF hoàn thành, bắt buộc resolution_note — HD07)
 *  RESOLVED       → CLOSED          (IT_MANAGER đóng hoặc auto sau 3 ngày)
 *  RESOLVED       → REOPEN_PENDING  (Employee yêu cầu mở lại trong 7 ngày / 168 giờ — HD10, BR-HD-04)
 *  REOPEN_PENDING → REOPENED        (IT_MANAGER duyệt mở lại)
 *  REOPEN_PENDING → RESOLVED        (IT_MANAGER từ chối mở lại)
 *  REOPENED       → IN_PROGRESS     (IT_STAFF tiếp tục xử lý sau khi được mở lại)
 *  REOPENED       → ASSIGNED        (IT_MANAGER phân công lại sau Reopen — SDD §2.1)
 *  CLOSED         → (không thể chuyển — terminal state)
 *
 * Ghi chú SLA:
 *  - Khi vé bị REOPENED: thêm SLA mới tính từ thời điểm reopen (BR-HD-04).
 *  - is_sla_breached KHÔNG bao giờ reset về false.
 */
public enum TicketStatus {

    /** Vé mới tạo, chưa được giao cho ai. */
    NEW,

    /** Đã được IT_MANAGER phân công cho IT_STAFF. */
    ASSIGNED,

    /** IT_STAFF đang xử lý. */
    IN_PROGRESS,

    /** IT_STAFF đã giải quyết xong. Đang chờ Employee xác nhận hoặc IT_MANAGER đóng. */
    RESOLVED,

    /** Đã đóng (terminal state). Không thể chuyển sang trạng thái khác. */
    CLOSED,

    /** Employee yêu cầu mở lại, đang chờ IT_MANAGER duyệt (BR-HD-04). */
    REOPEN_PENDING,

    /** IT_MANAGER đã duyệt mở lại. IT_STAFF tiếp tục xử lý. */
    REOPENED
}
