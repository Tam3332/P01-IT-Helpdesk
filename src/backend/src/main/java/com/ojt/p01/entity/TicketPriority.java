package com.ojt.p01.entity;

/**
 * Enum mức độ ưu tiên vé (type-safe thay cho String tự do).
 * Tương ứng trực tiếp với SLA config.
 *
 * Quy tắc Priority Triage:
 * impactLevel từ request → priority (do Employee tự khai báo mức ảnh hưởng).
 * IT_MANAGER có thể override priority sau khi xem xét.
 */
public enum TicketPriority {

 /** Ảnh hưởng thấp, giải quyết trong 48h. */
    LOW,

 /** Ảnh hưởng trung bình, giải quyết trong 24h. */
    MEDIUM,

 /** Ảnh hưởng cao, giải quyết trong 8h. */
    HIGH,

 /** Khẩn cấp ảnh hưởng toàn công ty, giải quyết trong 4h. */
    CRITICAL
}
